package com.buddy.studyguard.common.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.buddy.studyguard.common.data.db.entity.StudySessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySessionDao {

    @Query("SELECT * FROM study_sessions ORDER BY startAt DESC")
    fun observeAll(): Flow<List<StudySessionEntity>>

    @Query("SELECT * FROM study_sessions WHERE startAt BETWEEN :start AND :end ORDER BY startAt DESC")
    fun observeBetween(start: Long, end: Long): Flow<List<StudySessionEntity>>

    @Query("SELECT COALESCE(SUM(durationMs), 0) FROM study_sessions WHERE startAt BETWEEN :start AND :end")
    suspend fun sumDurationBetween(start: Long, end: Long): Long

    @Query("SELECT subject, SUM(durationMs) AS durationMs FROM study_sessions WHERE startAt BETWEEN :start AND :end GROUP BY subject")
    suspend fun sumBySubject(start: Long, end: Long): List<SubjectDuration>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: StudySessionEntity): Long

    @Query("DELETE FROM study_sessions WHERE id = :id")
    suspend fun deleteById(id: Long)
}

data class SubjectDuration(
    val subject: String,
    val durationMs: Long
)
