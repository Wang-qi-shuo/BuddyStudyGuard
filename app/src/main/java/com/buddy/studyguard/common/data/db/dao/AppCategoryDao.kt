package com.buddy.studyguard.common.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.buddy.studyguard.common.data.db.entity.AppCategoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AppCategoryDao {

    @Query("SELECT * FROM app_categories")
    fun observeAll(): Flow<List<AppCategoryEntity>>

    @Query("SELECT * FROM app_categories WHERE category = :category")
    suspend fun getByCategory(category: String): List<AppCategoryEntity>

    @Query("SELECT * FROM app_categories WHERE packageName = :pkg")
    suspend fun get(pkg: String): AppCategoryEntity?

    @Query("SELECT packageName FROM app_categories WHERE category = :category")
    suspend fun packagesOf(category: String): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(category: AppCategoryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(categories: List<AppCategoryEntity>)

    @Query("DELETE FROM app_categories WHERE packageName = :pkg")
    suspend fun delete(pkg: String)
}
