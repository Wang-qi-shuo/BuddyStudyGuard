package com.buddy.studyguard.study.ui.usage

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
import com.buddy.studyguard.common.data.db.entity.AppCategory
import com.buddy.studyguard.common.data.db.entity.AppUsageRecordEntity
import com.buddy.studyguard.common.ui.theme.NeonCyan
import com.buddy.studyguard.common.ui.theme.NeonMagenta
import com.buddy.studyguard.common.util.TimeUtil
import com.buddy.studyguard.study.ui.components.PixelCard

@Composable
fun UsageOverviewScreen(viewModel: UsageOverviewViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 总览卡片
        item {
            PixelCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "今日使用总时长",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = TimeUtil.formatGameTime(uiState.totalMs),
                    style = MaterialTheme.typography.headlineMedium,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "游戏时长 ${TimeUtil.formatGameTime(uiState.gameTotalMs)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NeonMagenta,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        item {
            Text(
                text = "应用明细（数据仅本地存储，不上传）",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }

        val maxMs = uiState.records.maxOfOrNull { it.foregroundMs } ?: 0L
        items(uiState.records, key = { "${it.packageName}_${it.day}" }) { rec ->
            UsageRow(rec, uiState.categoryMap[rec.packageName]?.label, uiState.categoryMap[rec.packageName]?.category, maxMs)
        }
    }
}

@Composable
private fun UsageRow(
    record: AppUsageRecordEntity,
    label: String?,
    category: String?,
    maxMs: Long
) {
    val isGame = category == AppCategory.GAME
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = label ?: record.packageName,
                style = MaterialTheme.typography.bodyMedium,
                color = if (isGame) NeonMagenta else MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = TimeUtil.formatGameTime(record.foregroundMs),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Canvas(modifier = Modifier.fillMaxWidth().height(8.dp).padding(top = 2.dp)) {
            val ratio = if (maxMs > 0) record.foregroundMs.toFloat() / maxMs.toFloat() else 0f
            drawRect(
                color = if (isGame) NeonMagenta else NeonCyan,
                topLeft = Offset.Zero,
                size = Size(size.width * ratio, size.height)
            )
        }
    }
}
