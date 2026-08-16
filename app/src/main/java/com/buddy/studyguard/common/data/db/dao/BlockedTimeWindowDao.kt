package com.buddy.studyguard.common.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.buddy.studyguard.common.data.db.entity.BlockedTimeWindowAppEntity
import com.buddy.studyguard.common.data.db.entity.BlockedTimeWindowEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BlockedTimeWindowDao {

    @Query("SELECT * FROM blocked_time_windows ORDER BY startMinute ASC")
    fun observeAll(): Flow<List<BlockedTimeWindowEntity>>

    @Query("SELECT * FROM blocked_time_windows WHERE enabled = 1")
    suspend fun getAllEnabled(): List<BlockedTimeWindowEntity>

    @Query("SELECT * FROM blocked_time_windows WHERE id = :id")
    suspend fun getById(id: Long): BlockedTimeWindowEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(window: BlockedTimeWindowEntity): Long

    @Update
    suspend fun update(window: BlockedTimeWindowEntity)

    @Query("DELETE FROM blocked_time_windows WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM blocked_time_window_apps WHERE windowId = :windowId")
    suspend fun clearApps(windowId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<BlockedTimeWindowAppEntity>)

    @Query("SELECT packageName FROM blocked_time_window_apps WHERE windowId = :windowId")
    suspend fun getAppsOf(windowId: Long): List<String>

    @Transaction
    suspend fun replaceApps(windowId: Long, packages: List<String>) {
        clearApps(windowId)
        insertApps(packages.map { BlockedTimeWindowAppEntity(windowId, it) })
    }

    @Query("DELETE FROM blocked_time_windows")
    suspend fun clearAllWindows()

    @Query("DELETE FROM blocked_time_window_apps")
    suspend fun clearAllApps()

    /** 清空全部时段及其关联应用（覆盖式同步用）。 */
    @Transaction
    suspend fun clearAll() {
        clearAllApps()
        clearAllWindows()
    }
}
