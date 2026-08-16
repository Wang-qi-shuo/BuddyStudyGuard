package com.buddy.studyguard.common.data.db.entity

import androidx.room.Entity

/**
 * 应用每日使用记录（按 packageName + day 唯一）。
 *
 * @param day 日期字符串 yyyy-MM-dd（系统默认时区）
 * @param foregroundMs 当天前台时长（毫秒）
 * @param launchCount 当天打开次数
 */
@Entity(tableName = "app_usage_records", primaryKeys = ["packageName", "day"])
data class AppUsageRecordEntity(
    val packageName: String,
    val day: String,
    val foregroundMs: Long,
    val launchCount: Int,
    val lastUpdated: Long = System.currentTimeMillis()
)
