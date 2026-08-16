package com.buddy.studyguard.common.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.buddy.studyguard.common.data.db.entity.AiMessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AiMessageDao {

    @Query("SELECT * FROM ai_messages WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    fun observeBySession(sessionId: String): Flow<List<AiMessageEntity>>

    @Query("SELECT * FROM ai_messages WHERE sessionId = :sessionId ORDER BY createdAt ASC")
    suspend fun getBySession(sessionId: String): List<AiMessageEntity>

    @Query("SELECT * FROM ai_messages ORDER BY createdAt DESC LIMIT :limit")
    suspend fun getLatest(limit: Int): List<AiMessageEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: AiMessageEntity): Long

    @Query("DELETE FROM ai_messages WHERE sessionId = :sessionId")
    suspend fun clearSession(sessionId: String)

    @Query("DELETE FROM ai_messages")
    suspend fun clearAll()
}
