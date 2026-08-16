package com.buddy.studyguard.study.ui.chat

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
 * 家长-学生聊天 ViewModel。
 * 管理消息列表状态、发送消息、已读标记、云端同步。
 */
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val chatRepository: ChatRepository,
    private val cloudSyncRepository: CloudSyncRepository,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"
        private const val PAGE_SIZE = 50
    }

    private val _messages = MutableStateFlow<List<ChatMessageEntity>>(emptyList())
    val messages: StateFlow<List<ChatMessageEntity>> = _messages.asStateFlow()

    private val _isSending = MutableStateFlow(false)
    val isSending: StateFlow<Boolean> = _isSending.asStateFlow()

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
        // 2. 从云端拉取历史消息并写入本地（异步，不影响 UI）
        viewModelScope.launch {
            syncFromCloud()
        }
    }

    /**
     * 从云端拉取消息并合并到本地 Room，同时建立实时监听。
     */
    private suspend fun syncFromCloud() {
        cloudSyncRepository.getFamilyId()
            .onSuccess { familyId ->
                // 拉取云端历史消息（分页）
                loadOlderMessagesInternal(familyId)

                // 3. 监听云端新增消息
                messageListener = cloudSyncRepository.listenMessages(familyId, viewModelScope) { doc ->
                    viewModelScope.launch {
                        try {
                            val idValue = doc["id"]
                            val cloudId = when (idValue) {
                                is Number -> idValue.toLong().toString()
                                is String -> idValue
                                else -> null
                            } ?: return@launch
                            val uid = doc["uid"] as? String ?: ""
                            // 跳过自己发送的消息（本地已存在）
                            val currentUid = CloudBaseManager.currentUserId()
                            if (uid == currentUid) return@launch
                            val content = doc["content"] as? String ?: ""
                            val senderType = doc["sender_type"] as? String
                                ?: ChatSenderType.STUDENT
                            val timestamp = (doc["timestamp"] as? Number)?.toLong()
                                ?: System.currentTimeMillis()
                            val senderName = doc["sender_name"] as? String ?: ""
                            val imageUri = resolveImageUri(doc)
                            val message = ChatMessageEntity(
                                senderType = senderType,
                                content = content,
                                imageUri = imageUri,
                                timestamp = timestamp,
                                isRead = false,
                                cloudId = cloudId,
                                senderUid = uid,
                                senderName = senderName
                            )
                            chatRepository.insertFromCloud(message)
                        } catch (e: Exception) {
                            Log.w(TAG, "监听消息处理失败: ${e.message}")
                        }
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
                    try {
                        val idValue = doc["id"]
                        val cloudId = when (idValue) {
                            is Number -> idValue.toLong().toString()
                            is String -> idValue
                            else -> null
                        } ?: continue
                        val content = doc["content"] as? String ?: ""
                        val senderType = doc["sender_type"] as? String
                            ?: ChatSenderType.STUDENT
                        val timestamp = (doc["timestamp"] as? Number)?.toLong()
                            ?: System.currentTimeMillis()
                        val senderUid = doc["uid"] as? String ?: ""
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
                                senderUid = senderUid,
                                senderName = senderName
                            )
                        )
                    } catch (e: Exception) {
                        Log.w(TAG, "合并云端消息失败: ${e.message}")
                    }
                }
                nextOffset += docs.size
                if (docs.size < PAGE_SIZE) hasMore = false
            }
            .onFailure { Log.w(TAG, "拉取云端消息失败: ${it.message}") }
        isLoadingOlder = false
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

    /** 发送消息（文本 + 可选图片 URI）。[isParent] 决定 [ChatSenderType]。 */
    fun sendMessage(content: String, imageUri: String? = null, isParent: Boolean) {
        val trimmed = content.trim()
        if (trimmed.isEmpty() && imageUri == null) return
        viewModelScope.launch {
            _isSending.value = true
            _sendError.value = null
            // 先写本地 Room
            chatRepository.sendMessage(
                senderType = if (isParent) ChatSenderType.PARENT else ChatSenderType.STUDENT,
                content = trimmed,
                imageUri = imageUri
            )
            // 再同步云端（异步，失败不影响本地）
            launch {
                val senderType = if (isParent) ChatSenderType.PARENT else ChatSenderType.STUDENT
                val imageBase64 = imageUri?.let {
                    withContext(Dispatchers.IO) {
                        ImageBase64.compressAndEncode(appContext, Uri.parse(it))
                    }
                }
                cloudSyncRepository.sendMessage(trimmed, senderType, imageBase64)
                    .onSuccess { _isSending.value = false }
                    .onFailure {
                        Log.w(TAG, "云端同步消息失败: ${it.message}")
                        _isSending.value = false
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

    /** 标记所有未读消息为已读。 */
    fun markAsRead() {
        viewModelScope.launch { chatRepository.markAllAsRead() }
    }

    override fun onCleared() {
        messageListener?.cancel()
        super.onCleared()
    }
}
