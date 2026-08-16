package com.buddy.studyguard.common.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.buddy.studyguard.common.data.db.entity.AppUsageRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppUsageRecordDao {

    @Query("SELECT * FROM app_usage_records WHERE day = :day ORDER BY foregroundMs DESC")
    fun observeByDay(day: String): Flow<List<AppUsageRecordEntity>>

    @Query("SELECT * FROM app_usage_records WHERE day BETWEEN :startDay AND :endDay ORDER BY day DESC, foregroundMs DESC")
    fun observeByDayRange(startDay: String, endDay: String): Flow<List<AppUsageRecordEntity>>

    @Query("SELECT * FROM app_usage_records WHERE packageName = :packageName AND day = :day")
    suspend fun get(packageName: String, day: String): AppUsageRecordEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(record: AppUsageRecordEntity)

    /**
     * 统计某天指定包名集合的总前台时长。
     * Room 会把 [packages] 绑定到 `IN (:packages)`。
     */
    @Query("SELECT COALESCE(SUM(foregroundMs), 0) FROM app_usage_records WHERE day = :day AND packageName IN (:packages)")
    suspend fun sumForegroundMsForPackages(day: String, packages: List<String>): Long

    @Query("DELETE FROM app_usage_records WHERE day < :day")
    suspend fun deleteBefore(day: String): Int
}
