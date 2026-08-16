package com.buddy.studyguard.monitor.worker

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.buddy.studyguard.common.data.db.dao.AppCategoryDao
import com.buddy.studyguard.common.data.db.dao.AppUsageRecordDao
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * 每日清理 Worker。
 *
 * 每天凌晨 0 点附近执行一次，删除 30 天前的应用使用记录，控制数据库体积。
 * 由 [BuddyStudyGuardApp] 中的 WorkManager（Hilt 注入的 [HiltWorkerFactory]）构造。
 */
@HiltWorker
class DailyResetWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val appUsageRecordDao: AppUsageRecordDao,
    private val appCategoryDao: AppCategoryDao
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val cutoff = LocalDate.now(ZoneId.systemDefault()).minusDays(RETENTION_DAYS.toLong())
            appUsageRecordDao.deleteBefore(cutoff.toString())
            cleanupOrphanCategories()
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "每日清理失败: ${e.message}", e)
            Result.retry()
        }
    }

    /**
     * 删除 app_categories 中已不在本机已安装应用集合里的孤儿分类记录。
     * 仅在能成功获取已安装应用列表时才执行，避免因列表获取失败误删全部分类。
     */
    private suspend fun cleanupOrphanCategories() {
        val installed = getInstalledPackages()
        if (installed.isEmpty()) {
            Log.w(TAG, "未获取到已安装应用列表，跳过分类清理")
            return
        }
        val orphans = appCategoryDao.observeAll().first()
            .filter { it.packageName !in installed }
        orphans.forEach { appCategoryDao.delete(it.packageName) }
        Log.d(TAG, "清理孤儿分类 ${orphans.size} 条")
    }

    private fun getInstalledPackages(): Set<String> {
        val pm = applicationContext.packageManager
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledApplications(0)
            }.mapNotNull { it.packageName }.toSet()
        } catch (e: Exception) {
            Log.w(TAG, "获取已安装应用列表失败: ${e.message}")
            emptySet()
        }
    }

    companion object {
        private const val TAG = "DailyResetWorker"
        private const val WORK_NAME = "daily_reset"
        private const val RETENTION_DAYS = 30

        /**
         * 调度每日清理任务（唯一工作，覆盖已存在的同名任务）。
         * 初始延迟设为下一个本地凌晨 0 点。
         */
        fun schedule(context: Context) {
            val minutesToMidnight = calcMinutesToNextMidnight()

            val request = PeriodicWorkRequestBuilder<DailyResetWorker>(
                1, TimeUnit.DAYS
            ).setInitialDelay(minutesToMidnight, TimeUnit.MINUTES).build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        /** 距离下一个本地凌晨 0 点的分钟数，范围 1..(24*60)。 */
        private fun calcMinutesToNextMidnight(): Long {
            val zone = ZoneId.systemDefault()
            val now = java.time.ZonedDateTime.now(zone)
            val nextMidnight = now.toLocalDate().plusDays(1).atStartOfDay(zone)
            return ChronoUnit.MINUTES.between(now, nextMidnight)
                .coerceIn(1L, 24L * 60L)
        }
    }
}
