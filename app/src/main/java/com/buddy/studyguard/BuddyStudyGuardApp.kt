package com.buddy.studyguard

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.buddy.studyguard.common.cloud.CloudBaseManager
import com.buddy.studyguard.common.cloud.RestrictionSyncWorker
import com.buddy.studyguard.common.cloud.UsageReportWorker
import com.buddy.studyguard.common.util.AutoCleanupManager
import com.buddy.studyguard.common.util.Constants
import com.buddy.studyguard.monitor.service.AppLimitForegroundService
import com.buddy.studyguard.monitor.worker.DailyResetWorker
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

/**
 * 应用入口。
 *
 * - `@HiltAndroidApp` 触发 Hilt 全量依赖图生成
 * - 实现 [Configuration.Provider] 以把 [HiltWorkerFactory] 接入 WorkManager
 *   （AndroidManifest 已移除 WorkManager 默认初始化，改由这里提供）
 * - onCreate 中创建通知渠道并调度每日清理任务
 */
@HiltAndroidApp
class BuddyStudyGuardApp : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    @Inject
    lateinit var autoCleanupManager: AutoCleanupManager

    override fun onCreate() {
        super.onCreate()
        CloudBaseManager.init(this)
        createNotificationChannels()
        DailyResetWorker.schedule(this)
        UsageReportWorker.schedule(this)
        RestrictionSyncWorker.schedule(this)
        AppLimitForegroundService.start(this)
        autoCleanupManager.start()
    }

    /** 创建前台服务 / 限制提醒 / 学习提醒三条通知渠道。 */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = getSystemService(NotificationManager::class.java)

        nm.createNotificationChannel(
            NotificationChannel(
                Constants.CHANNEL_FOREGROUND,
                "监护服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "保持学习监护后台运行" }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                Constants.CHANNEL_LIMIT_ALERT,
                "限制提醒",
                NotificationManager.IMPORTANCE_HIGH
            ).apply { description = "游戏超时或禁用时段提醒" }
        )
        nm.createNotificationChannel(
            NotificationChannel(
                Constants.CHANNEL_STUDY_REMINDER,
                "学习提醒",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = "家长消息与任务提醒" }
        )
    }
}
