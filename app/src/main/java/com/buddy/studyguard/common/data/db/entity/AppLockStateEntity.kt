package com.buddy.studyguard.common.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 即时锁定状态：被锁应用无法打开（家长一键锁定）。
 */
@Entity(tableName = "app_lock_states")
data class AppLockStateEntity(
    @PrimaryKey val packageName: String,
    val locked: Boolean,
    val lockedAt: Long = System.currentTimeMillis()
)
