package com.buddy.studyguard.common.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.buddy.studyguard.common.data.db.entity.ParentMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ParentMessageDao {

    /** 最新一条未关闭的消息（孩子模式首页卡片用）。 */
    @Query("SELECT * FROM parent_messages WHERE dismissed = 0 ORDER BY createdAt DESC LIMIT 1")
    fun observeLatestActive(): Flow<ParentMessageEntity?>

    @Query("SELECT * FROM parent_messages ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<ParentMessageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: ParentMessageEntity): Long

    @Update
    suspend fun update(message: ParentMessageEntity)

    @Query("UPDATE parent_messages SET dismissed = 1 WHERE id = :id")
    suspend fun dismiss(id: Long)
}
