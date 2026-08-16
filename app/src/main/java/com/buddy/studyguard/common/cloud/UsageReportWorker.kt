package com.buddy.studyguard.common.cloud

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
import com.buddy.studyguard.common.util.AppClassifier
import com.buddy.studyguard.common.util.TimeUtil
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.ZoneId
import java.util.concurrent.TimeUnit

/**
 * 定期将本地应用使用记录上报到云端的 WorkManager Worker。
 *
 * 每 2 小时执行一次，读取今天的 [AppUsageRecordEntity] 并逐条调用
 * [CloudSyncRepository.reportAppUsage] 同步到 CloudBase。
 */
@HiltWorker
class UsageReportWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val appUsageRecordDao: AppUsageRecordDao,
    private val appCategoryDao: AppCategoryDao,
    private val cloudSyncRepository: CloudSyncRepository,
    private val installedAppReporter: InstalledAppReporter
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "UsageReportWorker"
        const val WORK_NAME = "usage_report"
        private const val RETENTION_DAYS = 30

        /**
         * 调度定期上报任务（每 2 小时）。
         * 应在 [BuddyStudyGuardApp.onCreate] 中调用。
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<UsageReportWorker>(
                2, TimeUnit.HOURS
            ).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }
    }

    override suspend fun doWork(): Result {
        val today = TimeUtil.todayDayString()
        Log.d(TAG, "开始上报今日 ($today) 应用使用数据……")

        return try {
            // 仅学生身份上报本机使用时长；先校验/刷新登录态
            if (!CloudBaseManager.ensureValidToken()) {
                Log.w(TAG, "登录态失效，跳过时长上报")
                return Result.success()
            }
            val identity = cloudSyncRepository.getCurrentIdentity().getOrNull()
            if (identity != InstalledAppReporter.IDENTITY_STUDENT) {
                Log.d(TAG, "当前账号身份为 $identity，跳过时长上报")
                return Result.success()
            }

            // 先上报已安装应用清单（仅学生身份会上报），供家长端展示弟弟应用
            installedAppReporter.syncIfStudent()

            // 顺带清理云端 30 天前的旧使用记录，避免 app_usage 无限累积
            cleanupOldCloudUsage()

            // 同时上报今天与昨天，补报上一周期最后 2 小时可能漏报的数据
            val yesterday = LocalDate.now(ZoneId.systemDefault()).minusDays(1).toString()
            val days = listOf(today, yesterday).distinct()

            var successCount = 0
            var failCount = 0
            for (day in days) {
                val records = appUsageRecordDao.observeByDay(day).first()
                if (records.isEmpty()) {
                    Log.d(TAG, "$day 无使用记录")
                    continue
                }
                for (record in records) {
                    val appName = resolveAppLabel(record.packageName)
                    val category = appCategoryDao.get(record.packageName)?.category
                        ?: AppClassifier.classify(record.packageName, appName)
                    cloudSyncRepository.reportAppUsage(
                        appName = appName,
                        duration = record.foregroundMs,
                        date = record.day,
                        packageName = record.packageName,
                        category = category
                    ).onSuccess { successCount++ }
                        .onFailure { e ->
                            failCount++
                            Log.w(TAG, "上报 $appName 失败: ${e.message}")
                        }
                }
            }
            Log.d(TAG, "上报完成：成功 $successCount，失败 $failCount")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "上报应用使用数据失败: ${e.message}", e)
            Result.retry()
        }
    }

    /**
     * 清理云端 30 天前的应用使用记录，避免 app_usage 无限累积。
     * 失败仅记录日志，不影响本次上报结果。
     */
    private suspend fun cleanupOldCloudUsage() {
        try {
            val familyCode = cloudSyncRepository.getFamilyId().getOrNull() ?: return
            val cutoff = LocalDate.now(ZoneId.systemDefault()).minusDays(RETENTION_DAYS.toLong()).toString()
            cloudSyncRepository.deleteAppUsageBefore(familyCode, cutoff)
                .onSuccess { Log.d(TAG, "已清理云端 $cutoff 之前的应用使用记录") }
                .onFailure { e -> Log.w(TAG, "清理云端旧使用记录失败: ${e.message}") }
        } catch (e: Exception) {
            Log.w(TAG, "清理云端旧使用记录异常: ${e.message}")
        }
    }

    private fun resolveAppLabel(pkg: String): String = try {
        val pm = applicationContext.packageManager
        val ai = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getApplicationInfo(pkg, 0)
        }
        pm.getApplicationLabel(ai).toString()
    } catch (e: Exception) {
        pkg
    }
}
