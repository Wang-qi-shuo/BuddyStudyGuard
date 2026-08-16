package com.buddy.studyguard.parent.ui.tasks

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.buddy.studyguard.common.data.db.entity.TaskEntity
import com.buddy.studyguard.study.ui.components.PixelCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ParentTaskScreen(viewModel: ParentTaskViewModel = hiltViewModel()) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var due by remember { mutableStateOf("") }

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "布置学习任务",
            style = MaterialTheme.typography.titleMedium,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary
        )
        OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("标题") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = subject, onValueChange = { subject = it }, label = { Text("科目（可选）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = desc, onValueChange = { desc = it }, label = { Text("描述（可选）") }, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = due, onValueChange = { due = it }, label = { Text("截止 MM-dd（可选）") }, singleLine = true, modifier = Modifier.fillMaxWidth())

        Button(
            onClick = {
                viewModel.createTask(title, desc, subject, parseDue(due))
                title = ""; subject = ""; desc = ""; due = ""
            },
            enabled = title.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) { Text(stringResource(R.string.add)) }

        Text(
            text = "已布置任务",
            style = MaterialTheme.typography.titleSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(tasks, key = { it.id }) { task ->
                TaskRow(task, onDelete = { viewModel.deleteTask(task.id) })
            }
        }
    }
}

@Composable
private fun TaskRow(task: TaskEntity, onDelete: () -> Unit) {
    PixelCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = task.title, style = MaterialTheme.typography.bodyLarge)
                val dueText = task.dueAt?.let { "截止 ${SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(it))}" } ?: ""
                val status = if (task.completed) "· 已完成" else "· 未完成"
                Text(
                    text = "${task.subject.ifBlank { "任务" }} $dueText $status",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
            }
        }
    }
}

/** "MM-dd" -> 今年该日 00:00 epoch 毫秒，失败返回 null。 */
private fun parseDue(text: String): Long? {
    if (text.isBlank()) return null
    return try {
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val year = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR)
        fmt.parse("$year-$text")?.time
    } catch (e: Exception) {
        null
    }
}
