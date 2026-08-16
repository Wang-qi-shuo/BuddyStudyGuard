package com.buddy.studyguard.monitor.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.buddy.studyguard.common.util.PermissionUtil
import com.buddy.studyguard.monitor.engine.BlockReason
import com.buddy.studyguard.monitor.engine.RestrictionDecision
import com.buddy.studyguard.monitor.engine.RestrictionEngine
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 无障碍监护服务。
 *
 * 仅监听 [AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED] 事件（已在
 * `accessibility_service_config.xml` 中限定），当目标应用切到前台时调用
 * [RestrictionEngine.evaluate] 判断是否拦截：
 *
 * - 命中拦截则执行 `performGlobalAction(GLOBAL_ACTION_HOME)` 把弟弟送回桌面
 * - 同时启动 [FullScreenAlertActivity] 展示拦截原因
 *
 * 不读取任何界面节点内容，仅依赖包名做拦截判断。
 */
@AndroidEntryPoint
class AppLimitAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var engine: RestrictionEngine

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onServiceConnected() {
        super.onServiceConnected()
        android.util.Log.i(TAG, "AppLimitAccessibilityService connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg.isBlank()) return
        // 忽略自身及系统 UI
        if (pkg == packageName) return
        if (pkg in SYSTEM_UI_PACKAGES) return

        android.util.Log.i(TAG, "EVENT pkg=$pkg")

        scope.launch {
            try {
                val decision = engine.evaluate(pkg)
                android.util.Log.i(TAG, "EVALUATE pkg=$pkg => $decision")
                if (decision is RestrictionDecision.Block) {
                    performGlobalAction(GLOBAL_ACTION_HOME)
                    startActivity(
                        Intent(this@AppLimitAccessibilityService, FullScreenAlertActivity::class.java).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            putExtra(FullScreenAlertActivity.EXTRA_REASON, decision.reason.name)
                            putExtra(FullScreenAlertActivity.EXTRA_PACKAGE, decision.packageName)
                        }
                    )
                }
            } catch (e: Exception) {
                android.util.Log.w(TAG, "evaluate failed for $pkg", e)
            }
        }
    }

    override fun onInterrupt() {
        // 保留为空，无操作
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
        // 保活：延迟几秒后重启前台监护服务，避免 MIUI 杀后台后监护链路整体失效。
        // 无障碍服务本身由系统管理，进程被回收后系统会自动重新绑定；
        // 前台服务重启后会在周期自检中确认无障碍是否存活并引导用户恢复。
        val appContext = applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            delay(RESTART_DELAY_MS)
            runCatching { AppLimitForegroundService.start(appContext) }
        }
    }

    companion object {
        private const val TAG = "AppLimitA11y"

        /** onDestroy 后延迟重启前台服务的等待时长。 */
        private const val RESTART_DELAY_MS = 3_000L

        /** 系统界面包名前缀（避免误拦截桌面/输入法/通知栏等）。 */
        private val SYSTEM_UI_PACKAGES = setOf(
            "com.android.systemui",
            "com.android.settings"
        )

        /** 判断本服务的无障碍开关是否已开启。 */
        fun isRunning(context: Context): Boolean =
            PermissionUtil.isAccessibilityEnabled(context)
    }
}
