package com.buddy.studyguard.ai.data.repository

import android.content.Context
import android.net.Uri
import com.buddy.studyguard.ai.data.local.FaqRepository
import com.buddy.studyguard.ai.data.remote.DoubaoApi
import com.buddy.studyguard.ai.data.remote.DoubaoContentPart
import com.buddy.studyguard.ai.data.remote.DoubaoImageUrl
import com.buddy.studyguard.ai.data.remote.DoubaoMessage
import com.buddy.studyguard.ai.data.remote.DoubaoRequest
import com.buddy.studyguard.common.data.db.dao.AiMessageDao
import com.buddy.studyguard.common.data.db.entity.AiMessageEntity
import com.buddy.studyguard.common.data.db.entity.AiRole
import com.buddy.studyguard.common.util.Constants
import com.buddy.studyguard.common.util.ImageBase64
import com.buddy.studyguard.common.util.NetworkUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 一次发送操作的统一结果。
 */
sealed class AiResult {
    /** 联网调用豆包成功。 */
    data class Online(val content: String) : AiResult()

    /** 离线 / 网络异常兜底，内容来自 [FaqRepository]。 */
    data class Offline(val content: String) : AiResult()

    /** 调用失败（如 HTTP 错误码、空内容），不写入 assistant 消息，允许用户重试。 */
    data class Error(val message: String) : AiResult()
}

/**
 * AI 对话仓库：统一管理豆包在线调用与离线 FAQ 兜底。
 *
 * 在线流程：插入 USER → 拼装上下文（system + 历史）→ 调用 [DoubaoApi] → 写入 ASSISTANT。
 * 离线流程：插入 USER → 取 [FaqRepository] 答案 → 写入 ASSISTANT（fromOfflineCache=true）。
 *
 * 网络异常（[IOException]）会降级到离线 FAQ，保证无网也能用。
 */
@Singleton
class AiChatRepository @Inject constructor(
    private val doubaoApi: DoubaoApi,
    private val aiMessageDao: AiMessageDao,
    @ApplicationContext private val context: Context,
    private val faqRepository: FaqRepository
) {

    /** 观察某会话下的全部消息（按时间升序）。 */
    fun observeMessages(sessionId: String): Flow<List<AiMessageEntity>> =
        aiMessageDao.observeBySession(sessionId)

    /** 生成一个新会话 ID。 */
    suspend fun newSessionId(): String = UUID.randomUUID().toString()

    /** 清空指定会话的全部消息。 */
    suspend fun clearSession(sessionId: String) = aiMessageDao.clearSession(sessionId)

    /**
     * 发送一条用户消息并获取 AI 回复。
     *
     * 步骤：
     * 1. 插入 USER 消息到本地；
     * 2. 若无网络：取离线 FAQ 答案，插入 ASSISTANT（fromOfflineCache=true），返回 [AiResult.Offline]；
     * 3. 否则拼装上下文（system prompt + 历史消息）调用豆包 API；
     * 4. 成功且内容非空：插入 ASSISTANT，返回 [AiResult.Online]；
     * 5. HTTP 错误：返回 [AiResult.Error]，不插入 ASSISTANT，允许重试；
     * 6. [IOException]：降级到离线 FAQ，返回 [AiResult.Offline]。
     */
    suspend fun sendMessage(sessionId: String, userText: String, imageUri: String? = null): AiResult {
        var imageBase64: String? = null
        var imageMime: String? = null
        val hasImage = !imageUri.isNullOrBlank()

        if (hasImage) {
            val encoded = ImageBase64.encode(context, Uri.parse(imageUri))
            if (encoded != null) {
                imageBase64 = encoded.first
                imageMime = encoded.second
            } else {
                return AiResult.Error("图片读取失败，请重试")
            }
        }

        val effectiveText = when {
            hasImage -> "[图片] $userText".trim()
            userText.isNotBlank() -> userText
            else -> ""
        }
        if (effectiveText.isBlank()) return AiResult.Error("内容不能为空")

        aiMessageDao.insert(
            AiMessageEntity(
                id = AiMessageEntity.Ids.AUTO,
                sessionId = sessionId,
                role = AiRole.USER,
                content = effectiveText,
                createdAt = System.currentTimeMillis(),
                fromOfflineCache = false
            )
        )
        return resolveAssistant(sessionId, effectiveText, imageBase64, imageMime)
    }

    /**
     * 重试上一条 USER 消息：不重复插入 USER，直接重新走一次回复解析。
     * 用于 [AiResult.Error] 后用户点击重试。
     */
    suspend fun retryLast(sessionId: String): AiResult {
        val history = aiMessageDao.getBySession(sessionId)
        val lastUser = history.lastOrNull { it.role == AiRole.USER }
            ?: return AiResult.Error("没有可重试的消息")
        return resolveAssistant(sessionId, lastUser.content)
    }

    /**
     * 解析 ASSISTANT 回复（在线 / 离线兜底 / 错误）。
     * 不重复插入 USER——USER 已在 [sendMessage] 中写入，[retryLast] 直接复用历史。
     */
    private suspend fun resolveAssistant(
        sessionId: String,
        userText: String,
        imageBase64: String? = null,
        imageMime: String? = null
    ): AiResult {
        // 离线兜底
        if (!NetworkUtil.isOnline(context)) {
            return offlineFallback(sessionId, userText)
        }

        // 拼装上下文：system + 历史消息（含刚写入的 USER）
        val history = aiMessageDao.getBySession(sessionId)
        val lastUserId = history.lastOrNull { it.role == AiRole.USER }?.id
        val messages = buildList {
            add(DoubaoMessage(role = "system", content = Constants.AI_SYSTEM_PROMPT))
            history.forEach { e ->
                when (e.role) {
                    AiRole.USER -> {
                        val content: Any = if (e.id == lastUserId && imageBase64 != null) {
                            buildImageContent(e.content, imageBase64, imageMime)
                        } else {
                            e.content
                        }
                        add(DoubaoMessage(role = "user", content = content))
                    }
                    AiRole.ASSISTANT -> add(DoubaoMessage(role = "assistant", content = e.content))
                    // SYSTEM 不存储，跳过
                }
            }
        }

        val model = if (imageBase64 != null) Constants.DOUBAO_VISION_MODEL else Constants.DOUBAO_DEFAULT_MODEL

        return try {
            val response = doubaoApi.chat(
                DoubaoRequest(model = model, messages = messages)
            )
            if (response.isSuccessful) {
                val content = response.body()?.choices?.firstOrNull()?.message?.content as? String
                if (!content.isNullOrBlank()) {
                    aiMessageDao.insert(
                        AiMessageEntity(
                            id = AiMessageEntity.Ids.AUTO,
                            sessionId = sessionId,
                            role = AiRole.ASSISTANT,
                            content = content,
                            createdAt = System.currentTimeMillis(),
                            fromOfflineCache = false
                        )
                    )
                    AiResult.Online(content)
                } else {
                    AiResult.Error("AI 返回内容为空，请重试")
                }
            } else {
                AiResult.Error("服务异常（${response.code()}）")
            }
        } catch (e: IOException) {
            // 网络在线时 IOException 多为请求体过大/超时/连接被重置，不应误报为断网；
            // 仅当确实离线时才降级到离线 FAQ。
            if (NetworkUtil.isOnline(context)) {
                AiResult.Error("网络请求失败，请重试")
            } else {
                offlineFallback(sessionId, userText)
            }
        } catch (e: Throwable) {
            AiResult.Error(e.message ?: "未知错误")
        }
    }

    /** 构建视觉模型的多模态 user 内容：可选文本片段 + 图片片段。 */
    private fun buildImageContent(text: String, base64: String, mime: String?): List<DoubaoContentPart> {
        val parts = mutableListOf<DoubaoContentPart>()
        if (text.isNotBlank()) {
            parts.add(DoubaoContentPart(type = "text", text = text))
        }
        val mimeType = mime ?: "image/jpeg"
        parts.add(
            DoubaoContentPart(
                type = "image_url",
                imageUrl = DoubaoImageUrl("data:$mimeType;base64,$base64")
            )
        )
        return parts
    }

    /** 离线兜底：取 FAQ 答案写入 ASSISTANT（标记 fromOfflineCache），返回 [AiResult.Offline]。 */
    private suspend fun offlineFallback(sessionId: String, userText: String): AiResult {
        val answer = faqRepository.answer(userText)
        aiMessageDao.insert(
            AiMessageEntity(
                id = AiMessageEntity.Ids.AUTO,
                sessionId = sessionId,
                role = AiRole.ASSISTANT,
                content = answer,
                createdAt = System.currentTimeMillis(),
                fromOfflineCache = true
            )
        )
        return AiResult.Offline(answer)
    }
}
