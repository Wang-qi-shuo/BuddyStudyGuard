package com.buddy.studyguard.monitor.service

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.buddy.studyguard.R
import com.buddy.studyguard.common.cloud.RestrictionSyncWorker
import com.buddy.studyguard.common.data.db.dao.AppCategoryDao
import com.buddy.studyguard.common.data.db.dao.AppUsageRecordDao
import com.buddy.studyguard.common.util.Constants
import com.buddy.studyguard.common.util.PermissionUtil
import com.buddy.studyguard.common.util.TimeUtil
import com.buddy.studyguard.monitor.usage.UsageStatsHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 前台监护服务。
 *
 * 启动后：
 * 1. 创建 [Constants.CHANNEL_FOREGROUND] 通知渠道并 startForeground，保活后台运行
 * 2. 周期（60 秒）调用 [UsageStatsHelper.refreshAndPersist] 刷新当日各应用使用记录
 *
 * 由 [BootReceiver] 在开机后自动启动，也可由 UI 主动调用 [start] / [stop]。
 *
 * 使用 `specialUse` 前台服务类型，已在 manifest 中声明子用途为「学习监护：统计应用使用时长并在超限时提醒」。
 */
@AndroidEntryPoint
class AppLimitForegroundService : Service() {

    @Inject
    lateinit var usageStatsHelper: UsageStatsHelper

    @Inject
    lateinit var appUsageRecordDao: AppUsageRecordDao

    @Inject
    lateinit var appCategoryDao: AppCategoryDao

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        createChannel()
        startForegroundCompat()
        scope.launch {
            var tick = 0
            while (isActive) {
                runCatching {
                    val day = TimeUtil.todayDayString()
                    usageStatsHelper.refreshAndPersist(day, appUsageRecordDao, appCategoryDao)
                }
                // 无障碍保活自检：未开启时尝试重启并弹通知引导用户恢复
                runCatching { checkAccessibilityAlive() }
                // 每 2 个周期（约 2 分钟）触发一次限制快照即时同步，
                // 作为 WorkManager 5 分钟轮询在 MIUI 省电策略下不执行的兜底，缩短家长设限生效延迟
                if (tick % 2 == 0) {
                    runCatching { RestrictionSyncWorker.runNow(this@AppLimitForegroundService) }
                }
                tick++
                delay(REFRESH_INTERVAL_MS)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // MIUI 清后台时任务被移除：进程此刻仍存活，直接重启前台服务；
        // 同时用 AlarmManager 兜底（setAndAllowWhileIdle 比 set 更抗省电策略/Doze），
        // 防止 MIUI 拦截即时启动导致服务未恢复。
        start(this)
        val restartIntent = Intent(applicationContext, AppLimitForegroundService::class.java)
        val pi = PendingIntent.getService(
            applicationContext, 0, restartIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val am = getSystemService(AlarmManager::class.java)
        try {
            am.setAndAllowWhileIdle(AlarmManager.RTC, System.currentTimeMillis() + 1000, pi)
        } catch (e: Exception) {
            Log.w(TAG, "setAndAllowWhileIdle 失败，降级 set: ${e.message}")
            am.set(AlarmManager.RTC, System.currentTimeMillis() + 1000, pi)
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    /**
     * 无障碍保活自检：若本应用的无障碍服务未开启（MIUI 杀后台/系统回收），
     * 尝试触发系统重新绑定，并弹通知引导用户前往无障碍设置页恢复。
     */
    private fun checkAccessibilityAlive() {
        if (PermissionUtil.isAccessibilityEnabled(this)) return
        Log.w(TAG, "无障碍服务未开启，尝试重启并引导用户")
        // 对"已启用但进程被回收"的服务，startService 可触发系统重新绑定；
        // 对"被系统/用户关闭"的服务无效（需用户手动开启），此处仅尝试并忽略异常。
        try {
            startService(Intent(this, AppLimitAccessibilityService::class.java))
        } catch (_: Exception) {
            // 无障碍服务需系统绑定，普通 startService 可能抛异常，忽略
        }
        notifyAccessibilityDisabled()
    }

    /** 弹通知引导用户重新开启无障碍服务。 */
    private fun notifyAccessibilityDisabled() {
        val nm = getSystemService(NotificationManager::class.java)
        val intent = PermissionUtil.accessibilitySettingsIntent()
        val pi = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, Constants.CHANNEL_LIMIT_ALERT)
            .setContentTitle("无障碍服务未开启")
            .setContentText("限制功能无法生效，点击前往开启")
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .build()
        nm.notify(NOTIFICATION_ACCESSIBILITY_ID, notification)
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                Constants.CHANNEL_FOREGROUND,
                getString(R.string.limit_foreground_notification_title),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.limit_foreground_notification_text)
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val piFlags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }
        val contentIntent = if (launchIntent != null) {
            PendingIntent.getActivity(this, 0, launchIntent, piFlags)
        } else null

        return NotificationCompat.Builder(this, Constants.CHANNEL_FOREGROUND)
            .setContentTitle(getString(R.string.limit_foreground_notification_title))
            .setContentText(getString(R.string.limit_foreground_notification_text))
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .also { if (contentIntent != null) it.setContentIntent(contentIntent) }
            .build()
    }

    private fun startForegroundCompat() {
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        private const val TAG = "AppLimitForegroundService"
        private const val NOTIFICATION_ID = 1001
        private const val NOTIFICATION_ACCESSIBILITY_ID = 1002
        private const val REFRESH_INTERVAL_MS = 60_000L

        /** 启动前台监护服务。 */
        fun start(context: Context) {
            val intent = Intent(context, AppLimitForegroundService::class.java)
            try {
                ContextCompat.startForegroundService(context, intent)
            } catch (e: IllegalStateException) {
                Log.w(TAG, "启动前台服务被禁止（可能处于后台）：${e.message}")
            } catch (e: Exception) {
                Log.w(TAG, "启动前台服务失败：${e.message}")
            }
        }

        /** 停止前台监护服务。 */
        fun stop(context: Context) {
            context.stopService(Intent(context, AppLimitForegroundService::class.java))
        }
    }
}
