package com.buddy.studyguard.parent.ui.reports

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buddy.studyguard.common.data.db.dao.SubjectDuration
import com.buddy.studyguard.common.ui.theme.NeonAmber
import com.buddy.studyguard.common.ui.theme.NeonGreen
import com.buddy.studyguard.common.ui.theme.NeonMagenta
import com.buddy.studyguard.common.ui.theme.NeonCyan
import com.buddy.studyguard.common.util.TimeUtil
import com.buddy.studyguard.study.ui.components.PixelCard
import com.buddy.studyguard.study.ui.stats.StatsRange

@Composable
fun ParentReportScreen(viewModel: ParentReportViewModel = hiltViewModel()) {
    val range by viewModel.range.collectAsStateWithLifecycle()
    val data by viewModel.data.collectAsStateWithLifecycle()

    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = if (range == StatsRange.TODAY) 0 else 1) {
            Tab(
                selected = range == StatsRange.TODAY,
                onClick = { viewModel.setRange(StatsRange.TODAY) },
                text = { Text("今日", fontFamily = FontFamily.Monospace) }
            )
            Tab(
                selected = range == StatsRange.WEEK,
                onClick = { viewModel.setRange(StatsRange.WEEK) },
                text = { Text("本周", fontFamily = FontFamily.Monospace) }
            )
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    PixelCard(modifier = Modifier.weight(1f)) {
                        Text("游戏时长", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(TimeUtil.msToReadable(data.gameMs), style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace, color = NeonAmber)
                    }
                    PixelCard(modifier = Modifier.weight(1f)) {
                        Text("任务完成", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("${data.completedTasks}/${data.totalTasks}", style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace, color = NeonGreen)
                    }
                }
            }

            item {
                Text("游戏明细", style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
            }
            if (data.gameDetails.isEmpty()) {
                item { Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp)) }
            } else {
                items(data.gameDetails) { game ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(game.appName, style = MaterialTheme.typography.bodyMedium)
                        Text(TimeUtil.msToReadable(game.durationMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            if (range == StatsRange.WEEK) {
                item {
                    Text("本周每日使用", style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
                }
                if (data.weeklyUsage.isEmpty()) {
                    item { Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp)) }
                } else {
                    val maxMs = data.weeklyUsage.maxOf { it.durationMs }
                    items(data.weeklyUsage) { day ->
                        DailyBar(day, maxMs)
                    }
                }
            }

            item {
                Text("学习时长分布", style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
            }
            if (data.studyBySubject.isEmpty()) {
                item { Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp)) }
            } else {
                val maxMs = data.studyBySubject.maxOf { it.durationMs }
                items(data.studyBySubject) { item ->
                    SubjectBar(item, maxMs)
                }
            }

            item {
                Text("应用使用明细", style = MaterialTheme.typography.titleMedium, fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.primary)
            }
            if (data.appUsageDetails.isEmpty()) {
                item { Text("暂无数据", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp)) }
            } else {
                items(data.appUsageDetails) { detail ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(detail.appName, style = MaterialTheme.typography.bodyMedium)
                            if (detail.restricted) {
                                Text("受限", style = MaterialTheme.typography.labelSmall, color = NeonMagenta, fontFamily = FontFamily.Monospace)
                            }
                        }
                        Text(TimeUtil.msToReadable(detail.durationMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyBar(item: DailyUsage, maxMs: Long) {
    val ratio = if (maxMs > 0) item.durationMs.toFloat() / maxMs.toFloat() else 0f

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("${item.label} ${item.date}", style = MaterialTheme.typography.bodyMedium)
            Text(TimeUtil.msToReadable(item.durationMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Canvas(modifier = Modifier.fillMaxWidth().height(10.dp).padding(top = 2.dp)) {
            drawRect(color = NeonCyan, topLeft = Offset.Zero, size = Size(size.width * ratio, size.height))
        }
    }
}

@Composable
private fun SubjectBar(item: SubjectDuration, maxMs: Long) {
    val colors = listOf(NeonCyan, NeonMagenta, NeonGreen, NeonAmber)
    val color = colors[(item.subject.hashCode() and 0x7FFFFFFF) % colors.size]
    val ratio = if (maxMs > 0) item.durationMs.toFloat() / maxMs.toFloat() else 0f

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(item.subject, style = MaterialTheme.typography.bodyMedium)
            Text(TimeUtil.msToReadable(item.durationMs), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Canvas(modifier = Modifier.fillMaxWidth().height(10.dp).padding(top = 2.dp)) {
            drawRect(color = color, topLeft = Offset.Zero, size = Size(size.width * ratio, size.height))
        }
    }
}
