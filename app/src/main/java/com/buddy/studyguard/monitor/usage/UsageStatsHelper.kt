package com.buddy.studyguard.monitor.usage

import android.app.usage.UsageEvents
import android.app.usage.UsageStats
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.buddy.studyguard.common.data.db.dao.AppCategoryDao
import com.buddy.studyguard.common.data.db.dao.AppUsageRecordDao
import com.buddy.studyguard.common.data.db.entity.AppUsageRecordEntity
import com.buddy.studyguard.common.util.AppClassifier
import com.buddy.studyguard.common.util.PermissionUtil
import com.buddy.studyguard.common.util.TimeUtil
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 应用使用统计封装。
 *
 * 通过 [UsageStatsManager] 读取各应用前台时长与打开次数，并提供落库方法
 * [refreshAndPersist] 给前台监护服务周期调用。
 *
 * 在缺少 `PACKAGE_USAGE_STATS` 权限时友好降级：所有查询返回空列表，绝不崩溃。
 */
@Singleton
class UsageStatsHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val usageStatsManager: UsageStatsManager? =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager

    private val packageManager: PackageManager = context.packageManager

    /** 单应用使用统计快照。 */
    data class AppUsageStat(
        val packageName: String,
        val label: String,
        val foregroundMs: Long,
        val launchCount: Int
    )

    /** 已安装应用信息。 */
    data class AppInfo(
        val packageName: String,
        val label: String
    )

    /** 取某天（本地时区）各应用的前台时长与打开次数，按前台时长降序排列。 */
    fun getDailyUsage(day: LocalDate): List<AppUsageStat> {
        val zone = ZoneId.systemDefault()
        val start = day.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = day.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        return getRangeUsage(start, end)
    }

    /** 取 [start, end) 时间段内各应用的前台时长与打开次数。 */
    fun getRangeUsage(start: Long, end: Long): List<AppUsageStat> {
        val usm = usageStatsManager ?: return emptyList()
        if (!PermissionUtil.hasUsageStats(context)) return emptyList()

        val stats: Map<String, UsageStats> = try {
            usm.queryAndAggregateUsageStats(start, end) ?: emptyMap()
        } catch (e: SecurityException) {
            return emptyList()
        } catch (e: Exception) {
            return emptyList()
        }

        // 用 UsageEvents 统计 MOVE_TO_FOREGROUND 事件数 = 打开次数
        val launchCountMap = mutableMapOf<String, Int>()
        try {
            val events = usm.queryEvents(start, end)
            val ev = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(ev)
                if (ev.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                    val pkg = ev.packageName ?: continue
                    launchCountMap[pkg] = (launchCountMap[pkg] ?: 0) + 1
                }
            }
        } catch (e: Exception) {
            // 事件查询失败时仍返回时长数据，仅打开次数归零
        }

        return stats.map { (_, usageStat) ->
            AppUsageStat(
                packageName = usageStat.packageName,
                label = getAppLabel(usageStat.packageName),
                foregroundMs = usageStat.totalTimeInForeground,
                launchCount = launchCountMap[usageStat.packageName] ?: 0
            )
        }.filter { it.foregroundMs > 0 || it.launchCount > 0 }
            .filter { it.packageName != context.packageName }
            .sortedByDescending { it.foregroundMs }
    }

    /** 列出可启动的应用（带 LAUNCHER intent-filter 的应用）。 */
    fun getInstalledApps(): List<AppInfo> {
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val resolveInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                intent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
        }
        return resolveInfos.mapNotNull { ri ->
            val pkg = ri.activityInfo?.packageName ?: return@mapNotNull null
            if (pkg == context.packageName) return@mapNotNull null
            val label = try {
                ri.loadLabel(packageManager).toString()
            } catch (e: Exception) {
                pkg
            }
            AppInfo(pkg, label)
        }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
    }

    /**
     * 取当天数据写入 [AppUsageRecordDao]，并对未分类的应用用 [AppClassifier.autoEntity]
     * 自动归类后写入 [AppCategoryDao]（仅插入不存在的，已存在的不覆盖以尊重家长手动设置）。
     *
     * @param day yyyy-MM-dd 格式日期字符串
     */
    suspend fun refreshAndPersist(
        day: String,
        dao: AppUsageRecordDao,
        categoryDao: AppCategoryDao
    ) {
        val localDate = try {
            LocalDate.parse(day)
        } catch (e: Exception) {
            return
        }
        val stats = getDailyUsage(localDate)
        val now = System.currentTimeMillis()
        stats.forEach { stat ->
            dao.upsert(
                AppUsageRecordEntity(
                    packageName = stat.packageName,
                    day = day,
                    foregroundMs = stat.foregroundMs,
                    launchCount = stat.launchCount,
                    lastUpdated = now
                )
            )
            if (categoryDao.get(stat.packageName) == null) {
                categoryDao.upsert(AppClassifier.autoEntity(stat.packageName, stat.label))
            }
        }
    }

    private fun getAppLabel(pkg: String): String = try {
        val ai = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getApplicationInfo(pkg, 0)
        }
        packageManager.getApplicationLabel(ai).toString()
    } catch (e: PackageManager.NameNotFoundException) {
        pkg
    } catch (e: Exception) {
        pkg
    }

    /**
     * 将毫秒格式化为 "X小时Y分钟" 或 "X分钟"。
     * 委托给 [TimeUtil.formatGameTime]。
     */
    fun formatDuration(totalMillis: Long): String = TimeUtil.formatGameTime(totalMillis)
}
