package com.buddy.studyguard.parent.ui.messages

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buddy.studyguard.common.cloud.CloudBaseManager
import com.buddy.studyguard.common.cloud.CloudSyncRepository
import com.buddy.studyguard.common.cloud.ImageBase64
import com.buddy.studyguard.common.cloud.PollingListener
import com.buddy.studyguard.common.data.db.entity.ChatMessageEntity
import com.buddy.studyguard.common.data.db.entity.ChatSenderType
import com.buddy.studyguard.common.data.repository.ChatRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * 家长端聊天 ViewModel。
 * 复用与学生端 ChatViewModel 相同的 ChatRepository + 云端 messages 表，
 * 实现家长端与学生端消息互通（家长固定以 PARENT 身份发送）。
 */
@HiltViewModel
class ParentMessageViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val cloudSyncRepository: CloudSyncRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    companion object {
        private const val TAG = "ParentMessageViewModel"
        private const val PAGE_SIZE = 50
    }

    private val _messages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val messages: StateFlow<List<ChatMessageEntity>> = _messages.asStateFlow()

    private val _sendError = MutableStateFlow<String?>(null)
    val sendError: StateFlow<String?> = _sendError.asStateFlow()

    private var messageListener: PollingListener? = null

    private var nextOffset = 0
    private var hasMore = true
    private var isLoadingOlder = false

    init {
        // 1. 本地 Room 流
        viewModelScope.launch {
            chatRepository.getAllMessages().collect { list ->
                _messages.value = list
            }
        }
        // 2. 从云端拉取历史消息并建立实时监听
        viewModelScope.launch { syncFromCloud() }
    }

    fun sendMessage(content: String, imageUri: String? = null) {
        val trimmed = content.trim()
        if (trimmed.isEmpty() && imageUri == null) return
        viewModelScope.launch {
            // 先写本地 Room（家长身份）
            chatRepository.sendMessage(
                senderType = ChatSenderType.PARENT,
                content = trimmed,
                imageUri = imageUri
            )
            // 再同步云端（异步，失败不影响本地）
            launch {
                _sendError.value = null
                val imageBase64 = imageUri?.let {
                    withContext(Dispatchers.IO) {
                        ImageBase64.compressAndEncode(appContext, Uri.parse(it))
                    }
                }
                cloudSyncRepository.sendMessage(trimmed, ChatSenderType.PARENT, imageBase64)
                    .onFailure {
                        Log.w(TAG, "云端同步消息失败: ${it.message}")
                        val msg = it.message ?: ""
                        _sendError.value = if (msg.contains("图片上传失败")) {
                            "图片发送失败：云端 messages 表缺少 image 列，请在 CloudBase 控制台执行 ALTER TABLE messages ADD COLUMN IF NOT EXISTS image TEXT（消息已按纯文本发送）"
                        } else {
                            "消息发送失败，请检查网络后重试"
                        }
                    }
            }
        }
    }

    private suspend fun syncFromCloud() {
        cloudSyncRepository.getFamilyId()
            .onSuccess { familyId ->
                loadOlderMessagesInternal(familyId)

                messageListener = cloudSyncRepository.listenMessages(familyId, viewModelScope) { doc ->
                    viewModelScope.launch {
                        insertCloudMessage(doc, skipSelf = true)
                    }
                }
            }
            .onFailure { Log.w(TAG, "获取 familyId 失败，跳过云端同步: ${it.message}") }
    }

    /** 上滑加载更早消息（供 UI 调用）。 */
    fun loadOlderMessages() {
        if (isLoadingOlder || !hasMore) return
        viewModelScope.launch {
            cloudSyncRepository.getFamilyId()
                .onSuccess { familyId -> loadOlderMessagesInternal(familyId) }
                .onFailure { Log.w(TAG, "获取 familyId 失败，无法加载更早消息: ${it.message}") }
        }
    }

    /** 分页拉取云端消息并合并到本地。 */
    private suspend fun loadOlderMessagesInternal(familyId: String) {
        if (isLoadingOlder || !hasMore) return
        isLoadingOlder = true
        cloudSyncRepository.fetchMessages(familyId, PAGE_SIZE, nextOffset)
            .onSuccess { docs ->
                for (doc in docs) {
                    insertCloudMessage(doc, skipSelf = false)
                }
                nextOffset += docs.size
                if (docs.size < PAGE_SIZE) hasMore = false
            }
            .onFailure { Log.w(TAG, "拉取云端消息失败: ${it.message}") }
        isLoadingOlder = false
    }

    private suspend fun insertCloudMessage(doc: Map<String, Any?>, skipSelf: Boolean) {
        try {
            val idValue = doc["id"]
            val cloudId = when (idValue) {
                is Number -> idValue.toLong().toString()
                is String -> idValue
                else -> null
            } ?: return
            val uid = doc["uid"] as? String ?: ""
            // 监听阶段跳过自己刚发送的消息（本地已存在）
            if (skipSelf && uid == CloudBaseManager.currentUserId()) return
            val content = doc["content"] as? String ?: ""
            val senderType = doc["sender_type"] as? String ?: ChatSenderType.STUDENT
            val timestamp = (doc["timestamp"] as? Number)?.toLong()
                ?: System.currentTimeMillis()
            val senderName = doc["sender_name"] as? String ?: ""
            val imageUri = resolveImageUri(doc)
            chatRepository.insertFromCloud(
                ChatMessageEntity(
                    senderType = senderType,
                    content = content,
                    imageUri = imageUri,
                    timestamp = timestamp,
                    isRead = false,
                    cloudId = cloudId,
                    senderUid = uid,
                    senderName = senderName
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "合并云端消息失败: ${e.message}")
        }
    }

    /** 解析云端 image(base64) 字段，解码落盘后返回本地 file:// URI；无图返回 null。 */
    private suspend fun resolveImageUri(doc: Map<String, Any?>): String? {
        val image = doc["image"] as? String ?: return null
        if (image.isBlank()) return null
        val cloudId = doc["id"]?.toString() ?: "img_${System.currentTimeMillis()}"
        return withContext(Dispatchers.IO) {
            ImageBase64.decodeAndSave(appContext, image, "msg_$cloudId.jpg")
        }
    }

    override fun onCleared() {
        messageListener?.cancel()
        super.onCleared()
    }
}
