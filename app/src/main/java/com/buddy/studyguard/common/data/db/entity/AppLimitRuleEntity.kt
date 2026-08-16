package com.buddy.studyguard.common.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 单应用每日使用时长上限规则。超时由 [com.buddy.studyguard.monitor.engine.RestrictionEngine] 判定。
 *
 * @param dailyLimitMs 每日允许前台时长（毫秒）；0 表示无限制
 */
@Entity(tableName = "app_limit_rules")
data class AppLimitRuleEntity(
    @PrimaryKey val packageName: String,
    val dailyLimitMs: Long,
    val enabled: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)
