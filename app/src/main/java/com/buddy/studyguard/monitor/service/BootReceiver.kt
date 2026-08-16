package com.buddy.studyguard.monitor.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * 开机自启接收器。
 *
 * 接收到 [Intent.ACTION_BOOT_COMPLETED] 后启动 [AppLimitForegroundService]，
 * 确保设备重启后仍能持续监护使用时长。
 *
 * 无需 Hilt：仅调用 `startService`，不访问数据库 / 引擎。
 */
class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            AppLimitForegroundService.start(context)
        }
    }
}
