package com.buddy.studyguard.common.util

import android.app.AppOpsManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.text.TextUtils

/**
 * 权限检查工具：所有敏感权限都在首次使用时向弟弟解释清楚用途（见 strings.xml）。
 */
object PermissionUtil {

    /** 应用使用统计权限（PACKAGE_USAGE_STATS）。 */
    fun hasUsageStats(context: Context): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        } else {
            @Suppress("DEPRECATION")
            appOps.checkOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(),
                context.packageName
            )
        }
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** 悬浮窗权限（SYSTEM_ALERT_WINDOW）。 */
    fun hasOverlay(context: Context): Boolean =
        Settings.canDrawOverlays(context)

    /** 无障碍服务是否已启用（针对本应用的 AppLimitAccessibilityService）。 */
    fun isAccessibilityEnabled(context: Context, serviceName: String = "com.buddy.studyguard/.monitor.service.AppLimitAccessibilityService"): Boolean {
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        // 系统存储的组件名可能是完整形式（包名/完整类名）或短形式（包名/.类名），
        // 统一规范成两种形式一起比对，避免开启后仍被判为未开启。
        val candidates = buildSet {
            add(serviceName)
            ComponentName.unflattenFromString(serviceName)?.let { cn ->
                add(cn.flattenToString())
                add(cn.flattenToShortString())
            }
        }
        val splitter = TextUtils.SimpleStringSplitter(':')
        splitter.setString(enabled)
        while (splitter.hasNext()) {
            val item = splitter.next()
            if (candidates.any { it.equals(item, ignoreCase = true) }) return true
        }
        return false
    }

    /** 通知权限（API 33+ 需要 POST_NOTIFICATIONS 运行时权限）。 */
    fun hasNotification(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true
    }

    // ===== 跳转设置页 =====

    fun usageStatsSettingsIntent(): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

    fun overlaySettingsIntent(context: Context): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }

    fun accessibilitySettingsIntent(): Intent =
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
}
