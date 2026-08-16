package com.buddy.studyguard.study.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buddy.studyguard.R
import com.buddy.studyguard.common.data.db.entity.CourseEntity
import com.buddy.studyguard.common.data.db.entity.TaskEntity
import com.buddy.studyguard.common.ui.components.NeonGlowBorder
import com.buddy.studyguard.common.ui.components.PixelCorner
import com.buddy.studyguard.common.ui.components.PixelDivider
import com.buddy.studyguard.common.ui.theme.BgDeepest
import com.buddy.studyguard.common.ui.theme.NeonAmber
import com.buddy.studyguard.common.ui.theme.NeonCyan
import com.buddy.studyguard.common.ui.theme.NeonGreen
import com.buddy.studyguard.common.ui.theme.NeonMagenta
import com.buddy.studyguard.common.ui.theme.TextPrimary
import com.buddy.studyguard.common.ui.theme.TextSecondary
import com.buddy.studyguard.common.ui.theme.neonBorder
import com.buddy.studyguard.common.ui.theme.neonGlow
import com.buddy.studyguard.common.util.PermissionUtil
import com.buddy.studyguard.common.util.TimeUtil
import com.buddy.studyguard.study.ui.components.PixelCard

/**
 * 孩子端首页 — CRT网格背景 + 四角像素装饰 + 霓虹任务卡片。
 */
@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    var hasUsageStats by remember { mutableStateOf(PermissionUtil.hasUsageStats(context)) }
    var hasAccessibility by remember { mutableStateOf(PermissionUtil.isAccessibilityEnabled(context)) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasUsageStats = PermissionUtil.hasUsageStats(context)
                hasAccessibility = PermissionUtil.isAccessibilityEnabled(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // CRT网格背景
        Image(
            painter = painterResource(id = R.drawable.bg_crt_grid),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.10f)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (!hasUsageStats || !hasAccessibility) {
                item(key = "permission_banner") {
                    PermissionBanner(
                        hasUsageStats = hasUsageStats,
                        hasAccessibility = hasAccessibility,
                        onOpenUsageStats = { context.startActivity(PermissionUtil.usageStatsSettingsIntent()) },
                        onOpenAccessibility = { context.startActivity(PermissionUtil.accessibilitySettingsIntent()) }
                    )
                }
            }

            // 家长消息卡片 — 带 NeonGlowBorder
            uiState.latestMessage?.let { msg ->
                item(key = "msg_${msg.id}") {
                    NeonGlowBorder(
                        borderColor = NeonMagenta.copy(alpha = 0.6f),
                        glowColor = NeonMagenta,
                        cornerRadius = 10.dp,
                        borderWidth = 1.5.dp,
                        glowWidth = 4.dp,
                        breathPeriodMs = 2500,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        PixelCard {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = NeonMagenta
                                )
                                Text(
                                    text = "家长消息",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontFamily = FontFamily.Monospace,
                                    color = NeonMagenta,
                                    modifier = Modifier.padding(start = 6.dp)
                                )
                            }
                            Text(
                                text = msg.content,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary,
                                modifier = Modifier.padding(top = 6.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                IconButton(onClick = { viewModel.dismissMessage(msg.id) }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = stringResource(R.string.cancel),
                                        tint = TextSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── 四角装饰 + 今日任务 ──
            item {
                PixelDivider(color = NeonCyan)
            }

            item {
                SectionTitle("今日任务", NeonCyan)
            }
            if (uiState.todayTasks.isEmpty()) {
                item { EmptyHint(stringResource(R.string.empty)) }
            } else {
                items(uiState.todayTasks, key = { it.id }) { task ->
                    PixelCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neonBorder(NeonGreen.copy(alpha = 0.4f), width = 1.dp, glowWidth = 2.dp)
                            .neonGlow(NeonGreen.copy(alpha = 0.1f), radius = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (task.subject.isNotBlank()) "[${task.subject}]" else "·",
                                style = MaterialTheme.typography.labelSmall,
                                color = NeonGreen,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }
            }

            // ── 今日课程 ──
            item {
                PixelDivider(color = NeonMagenta)
            }
            item {
                SectionTitle("今日课程", NeonMagenta)
            }
            if (uiState.todayCourses.isEmpty()) {
                item { EmptyHint("今天没课") }
            } else {
                items(uiState.todayCourses, key = { "${it.dayOfWeek}_${it.period}" }) { course ->
                    PixelCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .neonBorder(NeonCyan.copy(alpha = 0.4f), width = 1.dp, glowWidth = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "第${course.period}节",
                                style = MaterialTheme.typography.labelMedium,
                                fontFamily = FontFamily.Monospace,
                                color = NeonCyan,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = course.subject,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                color = TextPrimary,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text(
                                text = "${TimeUtil.minuteToHm(course.startMinute)}-${TimeUtil.minuteToHm(course.endMinute)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }

            // ── 底部四角装饰 ──
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                ) {
                    PixelCorner(
                        color = NeonCyan.copy(alpha = 0.4f),
                        armLength = 20.dp,
                        thickness = 2.dp,
                        modifier = Modifier.matchParentSize()
                    )
                    Text(
                        text = "· 弟管严 v1.0 PIXEL ·",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = TextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.align(Alignment.Center).padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontFamily = FontFamily.Monospace,
        color = color,
        modifier = Modifier.neonGlow(color.copy(alpha = 0.2f), radius = 6.dp)
    )
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = TextSecondary,
        fontFamily = FontFamily.Monospace,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun PermissionBanner(
    hasUsageStats: Boolean,
    hasAccessibility: Boolean,
    onOpenUsageStats: () -> Unit,
    onOpenAccessibility: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .neonBorder(NeonAmber.copy(alpha = 0.5f), width = 1.dp, glowWidth = 2.dp)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (!hasUsageStats) {
            PermissionRow(
                text = "请开启「使用情况访问」权限，以便统计应用使用时长",
                onClick = onOpenUsageStats
            )
        }
        if (!hasAccessibility) {
            PermissionRow(
                text = "请开启无障碍服务，以便拦截受限应用",
                onClick = onOpenAccessibility
            )
        }
    }
}

@Composable
private fun PermissionRow(text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = NeonAmber,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onClick) {
            Text(
                text = "去开启",
                fontFamily = FontFamily.Monospace,
                color = NeonAmber
            )
        }
    }
}
