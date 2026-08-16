package com.buddy.studyguard.common.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 禁用时段（如 22:00-06:00 禁止打开指定应用）。
 *
 * @param startMinute 开始时间（当天内分钟数，0-1439）
 * @param endMinute 结束时间（分钟数；若 endMinute <= startMinute 表示跨天，如 22:00-06:00）
 * @param daysOfWeek 生效星期（位掩码，bit0=周日，bit1=周一，… bit6=周六；0x7F 表示每天）
 * @param appliesToAllApps 是否对所有受限应用生效；若 false 则仅对 [BlockedTimeWindowAppEntity] 中列出的包名生效
 */
@Entity(tableName = "blocked_time_windows")
data class BlockedTimeWindowEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val label: String = "",
    val startMinute: Int,
    val endMinute: Int,
    val daysOfWeek: Int = 0x7F,
    val appliesToAllApps: Boolean = true,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
