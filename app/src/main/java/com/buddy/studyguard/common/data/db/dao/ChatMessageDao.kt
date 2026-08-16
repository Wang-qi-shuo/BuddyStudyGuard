package com.buddy.studyguard.common.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.buddy.studyguard.common.data.db.entity.ChatMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {

    /** 按时间升序获取全部消息。 */
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllMessages(): Flow<List<ChatMessageEntity>>

    /** 插入一条新消息。 */
    @Insert
    suspend fun insert(message: ChatMessageEntity): Long

    /** 将所有未读消息标记为已读。 */
    @Query("UPDATE chat_messages SET isRead = 1 WHERE isRead = 0")
    suspend fun markAsRead()

    /** 观察未读消息数量。 */
    @Query("SELECT COUNT(*) FROM chat_messages WHERE isRead = 0")
    fun getUnreadCount(): Flow<Int>

    /** 按云端 ID 查找消息（用于去重）。 */
    @Query("SELECT * FROM chat_messages WHERE cloudId = :cloudId LIMIT 1")
    suspend fun getByCloudId(cloudId: String): ChatMessageEntity?

    /** 更新本地消息的云端 ID。 */
    @Query("UPDATE chat_messages SET cloudId = :cloudId WHERE id = :id")
    suspend fun setCloudId(id: Long, cloudId: String)
}
