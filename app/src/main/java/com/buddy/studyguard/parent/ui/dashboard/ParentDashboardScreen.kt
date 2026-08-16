package com.buddy.studyguard.parent.ui.dashboard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buddy.studyguard.R
import com.buddy.studyguard.common.data.db.entity.AppCategory
import com.buddy.studyguard.common.data.db.entity.AppUsageRecordEntity
import com.buddy.studyguard.common.ui.components.PixelDivider
import com.buddy.studyguard.common.ui.theme.BgCard
import com.buddy.studyguard.common.ui.theme.BgDeepest
import com.buddy.studyguard.common.ui.theme.NeonAmber
import com.buddy.studyguard.common.ui.theme.NeonCyan
import com.buddy.studyguard.common.ui.theme.NeonMagenta
import com.buddy.studyguard.common.ui.theme.PixelButtonStyles
import com.buddy.studyguard.common.ui.theme.TextPrimary
import com.buddy.studyguard.common.ui.theme.TextSecondary
import com.buddy.studyguard.common.ui.theme.neonBorder
import com.buddy.studyguard.common.ui.theme.neonGlow
import com.buddy.studyguard.common.util.TimeUtil
import com.buddy.studyguard.study.ui.components.PixelCard

/**
 * 家长仪表盘 — CRT网格背景 + 霓虹统计 + 像素分隔。
 */
@Composable
fun ParentDashboardScreen(
    onNavigate: (String) -> Unit,
    viewModel: ParentDashboardViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 0.dp)
    ) {
        // CRT网格背景
        Image(
            painter = painterResource(id = R.drawable.bg_crt_grid),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.12f)
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── 统计卡片行 ──
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        "今日游戏",
                        TimeUtil.msToReadable(uiState.todayGameMs),
                        NeonAmber,
                        Modifier.weight(1f)
                    )
                    StatCard(
                        "今日学习",
                        TimeUtil.msToReadable(uiState.todayStudyMs),
                        NeonCyan,
                        Modifier.weight(1f)
                    )
                }
            }
            item {
                StatCard(
                    "本周学习",
                    TimeUtil.msToReadable(uiState.weekStudyMs),
                    NeonCyan,
                    Modifier.fillMaxWidth()
                )
            }

            // ── 像素分隔 ──
            item { PixelDivider(color = NeonCyan) }

            // ── 快捷入口 ──
            item {
                Text(
                    text = "快捷入口",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    color = NeonCyan,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NavButton(stringResource(R.string.nav_parent_apps)) { onNavigate("parent_apps") }
                    NavButton(stringResource(R.string.nav_parent_messages)) { onNavigate("parent_messages") }
                }
            }
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NavButton(stringResource(R.string.nav_parent_tasks)) { onNavigate("parent_tasks") }
                    NavButton(stringResource(R.string.nav_parent_reports)) { onNavigate("parent_reports") }
                }
            }

            // ── 像素分隔 ──
            item { PixelDivider(color = NeonMagenta) }

            // ── 家庭聊天卡片 ──
            item {
                PixelCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neonBorder(NeonCyan, width = 1.dp, glowWidth = 3.dp)
                        .neonGlow(NeonCyan.copy(alpha = 0.15f), radius = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Chat,
                                contentDescription = null,
                                tint = NeonCyan,
                            )
                            Text(
                                text = "家庭聊天",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = NeonCyan,
                                modifier = Modifier.padding(start = 10.dp)
                            )
                        }
                        TextButton(
                            onClick = { onNavigate("parent_chat") },
                            shape = PixelButtonStyles.Shape,
                            border = BorderStroke(1.dp, NeonCyan),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = NeonCyan,
                            ),
                        ) {
                            Text(
                                text = "进入",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            // ── 修改秘钥卡片 ──
            item {
                PixelCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .neonBorder(NeonMagenta, width = 1.dp, glowWidth = 3.dp)
                        .neonGlow(NeonMagenta.copy(alpha = 0.15f), radius = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = NeonMagenta,
                            )
                            Text(
                                text = "修改家长秘钥",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                                color = NeonMagenta,
                                modifier = Modifier.padding(start = 10.dp)
                            )
                        }
                        TextButton(
                            onClick = { onNavigate("parent_pin") },
                            shape = PixelButtonStyles.Shape,
                            border = BorderStroke(1.dp, NeonMagenta),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = NeonMagenta,
                            ),
                        ) {
                            Text(
                                text = "进入",
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                    Text(
                        text = "默认秘钥，建议尽快修改",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF8888AA),
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(start = 34.dp, bottom = 4.dp)
                    )
                }
            }

            // ── 像素分隔 ──
            item { PixelDivider(color = NeonCyan) }

            // 应用排行标题
            item {
                Text(
                    text = "应用使用排行",
                    style = MaterialTheme.typography.titleMedium,
                    fontFamily = FontFamily.Monospace,
                    color = NeonCyan
                )
            }
            items(uiState.topApps, key = { "${it.packageName}_${it.day}" }) { rec ->
                AppRankRow(
                    rec,
                    uiState.categoryMap[rec.packageName]?.label,
                    uiState.categoryMap[rec.packageName]?.category
                )
            }
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    PixelCard(
        modifier = modifier
            .neonBorder(color.copy(alpha = 0.4f), width = 1.dp, glowWidth = 2.dp)
            .neonGlow(color.copy(alpha = 0.1f), radius = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.neonGlow(color.copy(alpha = 0.3f), radius = 8.dp)
        )
    }
}

@Composable
private fun RowScope.NavButton(text: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .neonBorder(NeonCyan.copy(alpha = 0.3f), width = 1.dp, glowWidth = 1.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.textButtonColors(
            containerColor = BgCard.copy(alpha = 0.6f),
            contentColor = TextPrimary
        )
    ) {
        Text(text, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun AppRankRow(record: AppUsageRecordEntity, label: String?, category: String?) {
    val isGame = category == AppCategory.GAME
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label ?: record.packageName,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isGame) NeonAmber else TextPrimary,
            fontFamily = FontFamily.Monospace
        )
        Text(
            text = TimeUtil.msToReadable(record.foregroundMs),
            style = MaterialTheme.typography.labelSmall,
            color = if (isGame) NeonAmber.copy(alpha = 0.7f) else TextSecondary.copy(alpha = 0.7f),
            fontFamily = FontFamily.Monospace
        )
    }
}
