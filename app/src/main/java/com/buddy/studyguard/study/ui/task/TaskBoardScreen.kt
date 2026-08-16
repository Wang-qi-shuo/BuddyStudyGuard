package com.buddy.studyguard.study.ui.task

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buddy.studyguard.R
import com.buddy.studyguard.common.data.db.entity.TaskEntity
import com.buddy.studyguard.common.data.db.entity.TaskSource
import com.buddy.studyguard.common.ui.theme.BgCard
import com.buddy.studyguard.common.ui.theme.NeonCyan
import com.buddy.studyguard.common.ui.theme.NeonMagenta
import com.buddy.studyguard.common.ui.theme.TextPrimary
import com.buddy.studyguard.common.ui.theme.TextSecondary
import com.buddy.studyguard.common.ui.theme.neonBorder
import com.buddy.studyguard.common.util.TimeUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun TaskBoardScreen(viewModel: TaskBoardViewModel = hiltViewModel()) {
    val tasks by viewModel.tasks.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var selectedTask by remember { mutableStateOf<TaskEntity?>(null) }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { showAdd = true },
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add)) }
            )
        }
    ) { inner ->
        if (tasks.isEmpty()) {
            Text(
                text = stringResource(R.string.empty),
                modifier = Modifier.padding(inner).padding(24.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            LazyColumn(
                modifier = Modifier.padding(inner).fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(tasks, key = { it.id }) { task ->
                    TaskItem(
                        task = task,
                        onToggle = { viewModel.toggleComplete(task) },
                        onDelete = { viewModel.deleteTask(task.id) },
                        onOpen = { selectedTask = task }
                    )
                }
            }
        }
    }

    if (showAdd) {
        AddTaskDialog(
            onConfirm = { title, desc, subject ->
                viewModel.addTask(title, desc, subject, dueAt = null)
                showAdd = false
            },
            onDismiss = { showAdd = false }
        )
    }

    selectedTask?.let { task ->
        TaskDetailDialog(task = task, onDismiss = { selectedTask = null })
    }
}

@Composable
private fun TaskItem(
    task: TaskEntity,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onOpen: () -> Unit
) {
    val isParentTask = task.source == TaskSource.PARENT
    val borderModifier = if (isParentTask) {
        Modifier.neonBorder(NeonMagenta, width = 1.dp, glowWidth = 4.dp)
    } else {
        Modifier
    }

    Surface(
        modifier = borderModifier.fillMaxWidth().clickable { onOpen() },
        shape = RoundedCornerShape(8.dp),
        color = if (isParentTask) BgCard else androidx.compose.ui.graphics.Color.Transparent,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(checked = task.completed, onCheckedChange = { onToggle() })
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = if (isParentTask) FontWeight.Bold else FontWeight.Normal,
                            color = if (isParentTask) TextPrimary else MaterialTheme.colorScheme.onSurface,
                        ),
                        textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isParentTask) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "家长布置",
                            tint = NeonMagenta,
                            modifier = Modifier.padding(start = 6.dp)
                        )
                    }
                }
                val sourceTag = if (isParentTask) "家长布置" else "自建"
                val dueTag = task.dueAt?.let { "· 截止 ${SimpleDateFormat("MM-dd", Locale.getDefault()).format(Date(it))}" } ?: ""
                Text(
                    text = "$sourceTag $dueTag${if (task.subject.isNotBlank()) " · ${task.subject}" else ""}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isParentTask) NeonMagenta else MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = if (isParentTask) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
            if (!isParentTask) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                }
            }
        }
    }
}

@Composable
private fun TaskDetailDialog(task: TaskEntity, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(task.title, fontFamily = FontFamily.Monospace) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DetailRow("具体要求", task.description.ifBlank { "（无）" })
                DetailRow("科目", task.subject.ifBlank { "（无）" })
                DetailRow(
                    "截止时间",
                    task.dueAt?.let {
                        SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(it))
                    } ?: "（无）"
                )
                DetailRow("完成状态", if (task.completed) "已完成" else "未完成")
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.confirm)) }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun AddTaskDialog(
    onConfirm: (title: String, desc: String, subject: String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf("") }
    var subject by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加任务", fontFamily = FontFamily.Monospace) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("标题") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("科目（可选）") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("描述（可选）") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(title, desc, subject) },
                enabled = title.isNotBlank()
            ) { Text(stringResource(R.string.confirm)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
