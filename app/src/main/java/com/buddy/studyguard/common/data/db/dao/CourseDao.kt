package com.buddy.studyguard.common.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.buddy.studyguard.common.data.db.entity.CourseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {

    @Query("SELECT * FROM courses ORDER BY dayOfWeek ASC, period ASC")
    fun observeAll(): Flow<List<CourseEntity>>

    @Query("SELECT * FROM courses WHERE dayOfWeek = :dayOfWeek ORDER BY period ASC")
    fun observeByDay(dayOfWeek: Int): Flow<List<CourseEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(course: CourseEntity)

    @Query("DELETE FROM courses WHERE dayOfWeek = :dayOfWeek AND period = :period")
    suspend fun delete(dayOfWeek: Int, period: Int)

    @Query("DELETE FROM courses")
    suspend fun clear()
}
