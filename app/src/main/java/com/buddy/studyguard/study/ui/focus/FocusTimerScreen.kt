package com.buddy.studyguard.study.ui.focus

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buddy.studyguard.common.data.db.entity.FocusMode

@Composable
fun FocusTimerScreen(viewModel: FocusTimerViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 模式切换
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = uiState.mode == FocusMode.POMODORO,
                onClick = { viewModel.setMode(FocusMode.POMODORO) },
                label = { Text("番茄钟") }
            )
            FilterChip(
                selected = uiState.mode == FocusMode.STOPWATCH,
                onClick = { viewModel.setMode(FocusMode.STOPWATCH) },
                label = { Text("正计时") }
            )
        }

        // 科目输入
        OutlinedTextField(
            value = uiState.currentSubject,
            onValueChange = { viewModel.setSubject(it) },
            label = { Text("学习科目") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        // 大号计时数字
        val displayMs = if (uiState.mode == FocusMode.POMODORO) uiState.remainingMs else uiState.elapsedMs
        Text(
            text = formatMs(displayMs),
            style = MaterialTheme.typography.displayMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(Modifier.height(16.dp))

        // 控制按钮
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (!uiState.running) {
                Button(onClick = { viewModel.start() }) { Text("开始") }
            } else {
                Button(onClick = { viewModel.pause() }) { Text("暂停") }
            }
            OutlinedButton(onClick = { viewModel.reset() }) { Text("重置") }
            OutlinedButton(onClick = { viewModel.finishAndSave() }) { Text("完成并保存") }
        }
    }
}

/** 毫秒 -> mm:ss 或 HH:mm:ss。 */
private fun formatMs(ms: Long): String {
    val totalSec = (ms / 1000).coerceAtLeast(0)
    val h = totalSec / 3600
    val m = (totalSec % 3600) / 60
    val s = totalSec % 60
    return if (h > 0) "%02d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}
