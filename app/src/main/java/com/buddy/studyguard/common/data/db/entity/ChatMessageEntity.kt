package com.buddy.studyguard.common.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 家长-学生聊天消息实体。
 *
 * @param senderType [ChatSenderType.PARENT] 家长发送 / [ChatSenderType.STUDENT] 学生发送
 * @param imageUri 图片 URI，纯文本消息时为 null
 * @param isRead 默认 false，家长进入聊天后由 [ChatMessageDao.markAsRead] 批量标记
 */
@Entity(tableName = "chat_messages")
data class ChatMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderType: String,
    val content: String,
    val imageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val cloudId: String? = null,
    val senderUid: String? = null,
    val senderName: String? = null
)

object ChatSenderType {
    const val PARENT = "PARENT"
    const val STUDENT = "STUDENT"
}
