package com.buddy.studyguard.common.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.buddy.studyguard.common.data.db.entity.ParentPinEntity

@Dao
interface ParentPinDao {

    @Query("SELECT * FROM parent_pin WHERE id = 1")
    suspend fun get(): ParentPinEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(pin: ParentPinEntity)
}
