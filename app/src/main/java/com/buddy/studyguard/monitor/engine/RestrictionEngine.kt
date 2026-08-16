package com.buddy.studyguard.monitor.engine

import com.buddy.studyguard.common.data.db.dao.AppLimitRuleDao
import com.buddy.studyguard.common.data.db.dao.AppLockStateDao
import com.buddy.studyguard.common.data.db.dao.AppUsageRecordDao
import com.buddy.studyguard.common.data.db.dao.BlockedTimeWindowDao
import com.buddy.studyguard.common.util.TimeUtil
import javax.inject.Inject
import javax.inject.Singleton

/** 限制评估结果：放行或拦截。 */
sealed class RestrictionDecision {
    /** 允许打开。 */
    object Allow : RestrictionDecision()

    /** 拦截并给出原因与目标包名。 */
    data class Block(val reason: BlockReason, val packageName: String) : RestrictionDecision()
}

/** 拦截原因。 */
enum class BlockReason {
    /** 单应用每日时长已用尽。 */
    TIME_LIMIT_EXCEEDED,

    /** 当前处于禁用时段（如夜间）。 */
    BLOCKED_TIME_WINDOW,

    /** 家长一键锁定。 */
    INSTANT_LOCKED
}

/**
 * 限制决策引擎。
 *
 * 给定前台应用包名，按以下顺序评估是否拦截：
 * 1. 即时锁定状态（[AppLockStateDao]）
 * 2. 单应用每日时长限制（[AppLimitRuleDao] + 当天累计前台时长）
 * 3. 禁用时段（[BlockedTimeWindowDao]，按星期掩码与分钟区间匹配）
 *
 * 任一命中即返回 [RestrictionDecision.Block]，全部不命中返回 [RestrictionDecision.Allow]。
 */
@Singleton
class RestrictionEngine @Inject constructor(
    private val appLockStateDao: AppLockStateDao,
    private val appLimitRuleDao: AppLimitRuleDao,
    private val blockedTimeWindowDao: BlockedTimeWindowDao,
    private val appUsageRecordDao: AppUsageRecordDao
) {

    /**
     * 评估 [foregroundPackage] 是否应被拦截。
     */
    suspend fun evaluate(foregroundPackage: String): RestrictionDecision {
        // 1. 即时锁定
        if (appLockStateDao.get(foregroundPackage)?.locked == true) {
            return RestrictionDecision.Block(BlockReason.INSTANT_LOCKED, foregroundPackage)
        }

        // 2. 每日时长限制
        val rule = appLimitRuleDao.get(foregroundPackage)
        if (rule != null && rule.enabled && rule.dailyLimitMs > 0) {
            val day = TimeUtil.todayDayString()
            val usedMs = appUsageRecordDao.sumForegroundMsForPackages(day, listOf(foregroundPackage))
            if (usedMs >= rule.dailyLimitMs) {
                return RestrictionDecision.Block(BlockReason.TIME_LIMIT_EXCEEDED, foregroundPackage)
            }
        }

        // 3. 禁用时段
        val now = System.currentTimeMillis()
        val nowMinute = TimeUtil.minuteOfDay(now)
        val windows = blockedTimeWindowDao.getAllEnabled()
        for (window in windows) {
            if (!TimeUtil.isTodayInMask(window.daysOfWeek, now)) continue
            if (!TimeUtil.isMinuteInWindow(nowMinute, window.startMinute, window.endMinute)) continue
            if (window.appliesToAllApps) {
                return RestrictionDecision.Block(BlockReason.BLOCKED_TIME_WINDOW, foregroundPackage)
            }
            val apps = blockedTimeWindowDao.getAppsOf(window.id)
            if (foregroundPackage in apps) {
                return RestrictionDecision.Block(BlockReason.BLOCKED_TIME_WINDOW, foregroundPackage)
            }
        }

        return RestrictionDecision.Allow
    }
}
