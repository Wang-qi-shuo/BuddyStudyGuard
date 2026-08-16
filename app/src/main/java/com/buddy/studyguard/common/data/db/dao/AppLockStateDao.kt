package com.buddy.studyguard.common.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.buddy.studyguard.common.data.db.entity.AppLockStateEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLockStateDao {

    @Query("SELECT * FROM app_lock_states")
    fun observeAll(): Flow<List<AppLockStateEntity>>

    @Query("SELECT * FROM app_lock_states WHERE locked = 1")
    suspend fun getAllLocked(): List<AppLockStateEntity>

    @Query("SELECT * FROM app_lock_states WHERE packageName = :pkg")
    suspend fun get(pkg: String): AppLockStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(state: AppLockStateEntity)

    @Query("UPDATE app_lock_states SET locked = :locked, lockedAt = :at WHERE packageName = :pkg")
    suspend fun setLocked(pkg: String, locked: Boolean, at: Long = System.currentTimeMillis())

    @Query("DELETE FROM app_lock_states WHERE packageName = :pkg")
    suspend fun delete(pkg: String)

    @Query("DELETE FROM app_lock_states")
    suspend fun clearAll()
}
