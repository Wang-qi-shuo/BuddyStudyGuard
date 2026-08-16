package com.buddy.studyguard.common.data.repository

import com.buddy.studyguard.common.data.db.dao.ChatMessageDao
import com.buddy.studyguard.common.data.db.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 家长-学生聊天仓库。
 * 负责消息的读写与已读状态管理。
 */
@Singleton
class ChatRepository @Inject constructor(
    private val chatMessageDao: ChatMessageDao
) {

    /** 观察全部消息（时间升序）。 */
    fun getAllMessages(): Flow<List<ChatMessageEntity>> =
        chatMessageDao.getAllMessages()

    /** 发送一条消息（文本 + 可选图片）。 */
    suspend fun sendMessage(
        senderType: String,
        content: String,
        imageUri: String? = null
    ) {
        chatMessageDao.insert(
            ChatMessageEntity(
                senderType = senderType,
                content = content,
                imageUri = imageUri,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    /** 将所有未读标记为已读。 */
    suspend fun markAllAsRead() = chatMessageDao.markAsRead()

    /** 观察未读数量。 */
    fun getUnreadCount(): Flow<Int> = chatMessageDao.getUnreadCount()

    /**
     * 从云端同步插入一条消息（带去重）。
     * 若 cloudId 已存在则跳过，避免重复插入。
     */
    suspend fun insertFromCloud(message: ChatMessageEntity) {
        if (message.cloudId != null) {
            val existing = chatMessageDao.getByCloudId(message.cloudId)
            if (existing != null) return
        }
        chatMessageDao.insert(message)
    }
}
