package com.buddy.studyguard.common.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * 禁用时段与包名的多对多关联（当 [BlockedTimeWindowEntity.appliesToAllApps] = false 时生效）。
 */
@Entity(
    tableName = "blocked_time_window_apps",
    primaryKeys = ["windowId", "packageName"],
    foreignKeys = [
        ForeignKey(
            entity = BlockedTimeWindowEntity::class,
            parentColumns = ["id"],
            childColumns = ["windowId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("windowId"), Index("packageName")]
)
data class BlockedTimeWindowAppEntity(
    val windowId: Long,
    val packageName: String
)
