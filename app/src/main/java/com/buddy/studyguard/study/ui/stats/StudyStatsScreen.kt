package com.buddy.studyguard.study.ui.stats

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buddy.studyguard.common.data.db.dao.SubjectDuration
import com.buddy.studyguard.common.ui.theme.NeonAmber
import com.buddy.studyguard.common.ui.theme.NeonCyan
import com.buddy.studyguard.common.ui.theme.NeonGreen
import com.buddy.studyguard.common.ui.theme.NeonMagenta
import com.buddy.studyguard.common.util.TimeUtil

@Composable
fun StudyStatsScreen(viewModel: StudyStatsViewModel = hiltViewModel()) {
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

        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "学习时长",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = TimeUtil.msToReadable(data.totalMs),
                style = MaterialTheme.typography.displaySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Text(
            text = "科目分布",
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )

        if (data.bySubject.isEmpty()) {
            Text(
                text = "暂无数据",
                modifier = Modifier.padding(24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(data.bySubject) { item ->
                    SubjectBar(item, data.bySubject.maxOf { it.durationMs })
                }
            }
        }
    }
}

@Composable
private fun SubjectBar(item: SubjectDuration, maxMs: Long) {
    val colors = listOf(NeonCyan, NeonMagenta, NeonGreen, NeonAmber)
    val color = colors[(item.subject.hashCode() and 0x7FFFFFFF) % colors.size]
    val ratio = if (maxMs > 0) item.durationMs.toFloat() / maxMs.toFloat() else 0f

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = item.subject, style = MaterialTheme.typography.bodyMedium)
            Text(
                text = TimeUtil.msToReadable(item.durationMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Canvas(
            modifier = Modifier.fillMaxWidth().height(12.dp).padding(top = 4.dp)
        ) {
            val w = size.width * ratio
            drawRect(
                color = color,
                topLeft = Offset.Zero,
                size = Size(w, size.height)
            )
        }
    }
}
