package com.buddy.studyguard.common.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.buddy.studyguard.common.data.db.entity.AppLimitRuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppLimitRuleDao {

    @Query("SELECT * FROM app_limit_rules")
    fun observeAll(): Flow<List<AppLimitRuleEntity>>

    @Query("SELECT * FROM app_limit_rules WHERE enabled = 1")
    suspend fun getAllEnabled(): List<AppLimitRuleEntity>

    @Query("SELECT * FROM app_limit_rules WHERE packageName = :pkg")
    suspend fun get(pkg: String): AppLimitRuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: AppLimitRuleEntity)

    @Query("DELETE FROM app_limit_rules WHERE packageName = :pkg")
    suspend fun delete(pkg: String)

    @Query("UPDATE app_limit_rules SET enabled = :enabled WHERE packageName = :pkg")
    suspend fun setEnabled(pkg: String, enabled: Boolean)

    @Query("DELETE FROM app_limit_rules")
    suspend fun clearAll()
}
