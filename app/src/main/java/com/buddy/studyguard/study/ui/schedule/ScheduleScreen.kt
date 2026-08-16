package com.buddy.studyguard.study.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buddy.studyguard.R
import com.buddy.studyguard.common.data.db.entity.CourseEntity
import com.buddy.studyguard.common.util.TimeUtil
import com.buddy.studyguard.study.ui.components.PixelCard

private val DAY_NAMES = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")

@Composable
fun ScheduleScreen(viewModel: ScheduleViewModel = hiltViewModel()) {
    val courses by viewModel.courses.collectAsStateWithLifecycle()
    val today = TimeUtil.dayOfWeek(System.currentTimeMillis())
    var selectedDay by remember { mutableIntStateOf(today) }
    var showAdd by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add)) }
            )
        }
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            TabRow(selectedTabIndex = selectedDay - 1) {
                DAY_NAMES.forEachIndexed { index, name ->
                    Tab(
                        selected = selectedDay - 1 == index,
                        onClick = { selectedDay = index + 1 },
                        text = {
                            Text(
                                text = if (index + 1 == today) "▶ $name" else name,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    )
                }
            }

            val dayCourses = courses.filter { it.dayOfWeek == selectedDay }.sortedBy { it.period }
            if (dayCourses.isEmpty()) {
                Text(
                    text = stringResource(R.string.empty),
                    modifier = Modifier.padding(24.dp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(dayCourses, key = { "${it.dayOfWeek}_${it.period}" }) { course ->
                        CourseCard(
                            course = course,
                            onDelete = { viewModel.delete(course.dayOfWeek, course.period) }
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddCourseDialog(
            dayOfWeek = selectedDay,
            onConfirm = { course -> viewModel.upsert(course); showAdd = false },
            onDismiss = { showAdd = false }
        )
    }
}

@Composable
private fun CourseCard(course: CourseEntity, onDelete: () -> Unit) {
    PixelCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "第${course.period}节",
                style = MaterialTheme.typography.titleMedium,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = course.subject,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 12.dp)
            )
            Text(
                text = "${TimeUtil.minuteToHm(course.startMinute)}-${TimeUtil.minuteToHm(course.endMinute)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                }
            }
        }
        if (course.note.isNotBlank()) {
            Text(
                text = course.note,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AddCourseDialog(
    dayOfWeek: Int,
    onConfirm: (CourseEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var period by remember { mutableStateOf("1") }
    var subject by remember { mutableStateOf("") }
    var start by remember { mutableStateOf("08:00") }
    var end by remember { mutableStateOf("08:45") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加课程 · ${DAY_NAMES[dayOfWeek - 1]}", fontFamily = FontFamily.Monospace) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = period, onValueChange = { period = it }, label = { Text("节次") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("科目") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = start, onValueChange = { start = it }, label = { Text("开始 HH:mm") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = end, onValueChange = { end = it }, label = { Text("结束 HH:mm") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val p = period.toIntOrNull() ?: return@TextButton
                    val s = parseHm(start) ?: return@TextButton
                    val e = parseHm(end) ?: return@TextButton
                    onConfirm(CourseEntity(dayOfWeek, p, subject.trim(), s, e))
                },
                enabled = subject.isNotBlank()
            ) { Text(stringResource(R.string.confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

/** "HH:mm" -> 分钟数，失败返回 null。 */
private fun parseHm(text: String): Int? {
    val parts = text.split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    return h * 60 + m
}
