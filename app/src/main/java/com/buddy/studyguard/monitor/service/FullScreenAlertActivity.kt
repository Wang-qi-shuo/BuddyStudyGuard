package com.buddy.studyguard.monitor.service

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.buddy.studyguard.R
import com.buddy.studyguard.common.ui.theme.BuddyStudyGuardTheme
import com.buddy.studyguard.monitor.engine.BlockReason
import dagger.hilt.android.AndroidEntryPoint

/**
 * 全屏拦截提醒 Activity。
 *
 * 由 [AppLimitAccessibilityService] 在命中 [BlockReason] 后启动，向弟弟展示
 * 被拦截原因，并提供「我知道了」按钮关闭。使用透明全屏主题，可在锁屏上显示
 * 并点亮屏幕（API 27+ 用 [setShowWhenLocked] / [setTurnScreenOn]，低版本用 window flags）。
 *
 * 不使用 `SYSTEM_ALERT_WINDOW` 悬浮窗方案，避免兼容性问题。
 */
@AndroidEntryPoint
class FullScreenAlertActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        val reasonName = intent?.getStringExtra(EXTRA_REASON) ?: ""
        val pkg = intent?.getStringExtra(EXTRA_PACKAGE) ?: ""
        val reason = runCatching { BlockReason.valueOf(reasonName) }.getOrNull()

        setContent {
            BuddyStudyGuardTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black.copy(alpha = 0.85f)
                ) {
                    AlertCard(reason = reason, onDismiss = { finish() })
                }
            }
        }
    }

    @Composable
    private fun AlertCard(reason: BlockReason?, onDismiss: () -> Unit) {
        val (titleRes, textRes) = when (reason) {
            BlockReason.TIME_LIMIT_EXCEEDED ->
                R.string.limit_time_up_title to R.string.limit_time_up_text
            BlockReason.BLOCKED_TIME_WINDOW ->
                R.string.limit_blocked_period_title to R.string.limit_blocked_period_text
            BlockReason.INSTANT_LOCKED ->
                R.string.limit_locked_title to R.string.limit_locked_text
            null ->
                R.string.limit_locked_title to R.string.limit_locked_text
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(32.dp)
                    .fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(titleRes),
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(textRes),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("我知道了")
                    }
                }
            }
        }
    }

    companion object {
        /** 拦截原因 [BlockReason.name]。 */
        const val EXTRA_REASON = "extra_reason"
        /** 被拦截的包名。 */
        const val EXTRA_PACKAGE = "extra_package"
    }
}
