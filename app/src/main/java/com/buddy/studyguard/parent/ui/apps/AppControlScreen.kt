package com.buddy.studyguard.parent.ui.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.buddy.studyguard.R
import com.buddy.studyguard.common.data.db.entity.AppCategory
import com.buddy.studyguard.common.data.db.entity.BlockedTimeWindowEntity
import com.buddy.studyguard.common.ui.theme.NeonAmber
import com.buddy.studyguard.common.ui.theme.NeonMagenta
import com.buddy.studyguard.common.util.TimeUtil

@Composable
fun AppControlScreen(viewModel: AppControlViewModel = hiltViewModel()) {
    val apps by viewModel.apps.collectAsStateWithLifecycle()
    val windows by viewModel.windows.collectAsStateWithLifecycle()
    val syncError by viewModel.syncError.collectAsStateWithLifecycle()
    var tab by remember { mutableIntStateOf(0) }
    var limitDialogFor by remember { mutableStateOf<AppControlItem?>(null) }
    var categoryDialogFor by remember { mutableStateOf<AppControlItem?>(null) }
    var showAddWindow by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            if (tab == 1) {
                ExtendedFloatingActionButton(
                    onClick = { showAddWindow = true },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    text = { Text(stringResource(R.string.add)) }
                )
            }
        }
    ) { inner ->
        Column(Modifier.padding(inner).fillMaxSize()) {
            if (syncError != null) {
                Surface(
                    color = NeonAmber.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = syncError!!,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = NeonAmber,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(onClick = { viewModel.clearSyncError() }) {
                            Text("知道了", fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                        }
                    }
                }
            }
            TabRow(selectedTabIndex = tab) {
                Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("应用限制", fontFamily = FontFamily.Monospace) })
                Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("禁用时段", fontFamily = FontFamily.Monospace) })
            }
            when (tab) {
                0 -> AppLimitList(
                    apps = apps,
                    onToggleLock = { viewModel.setLocked(it.packageName, !it.locked) },
                    onSetLimit = { limitDialogFor = it },
                    onSetCategory = { categoryDialogFor = it },
                    onRefresh = { viewModel.refreshChildApps() }
                )
                1 -> WindowList(
                    windows = windows,
                    onDelete = { viewModel.deleteWindow(it) }
                )
            }
        }
    }

    limitDialogFor?.let { item ->
        LimitDialog(
            currentMinutes = if (item.limitMs > 0) (item.limitMs / 60_000).toInt() else 0,
            onConfirm = { min -> viewModel.setLimitMinutes(item.packageName, min); limitDialogFor = null },
            onDismiss = { limitDialogFor = null }
        )
    }
    categoryDialogFor?.let { item ->
        CategoryDialog(
            currentCategory = item.category,
            onConfirm = { category ->
                viewModel.setCategory(item.packageName, item.label, category)
                categoryDialogFor = null
            },
            onDismiss = { categoryDialogFor = null }
        )
    }
    if (showAddWindow) {
        AddWindowDialog(
            apps = apps,
            onConfirm = { label, s, e, days, appliesToAll, packages ->
                viewModel.addWindow(label, s, e, days, appliesToAll, packages)
                showAddWindow = false
            },
            onDismiss = { showAddWindow = false }
        )
    }
}

@Composable
private fun AppLimitList(
    apps: List<AppControlItem>,
    onToggleLock: (AppControlItem) -> Unit,
    onSetLimit: (AppControlItem) -> Unit,
    onSetCategory: (AppControlItem) -> Unit,
    onRefresh: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onRefresh) {
                    Icon(Icons.Default.Refresh, contentDescription = "刷新")
                    Spacer(Modifier.width(4.dp))
                    Text("刷新")
                }
            }
        }
        items(apps, key = { it.packageName }) { app ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (app.isGame) NeonAmber else MaterialTheme.colorScheme.onSurface
                    )
                    val tag = when (app.category) {
                        AppCategory.GAME -> "游戏"
                        AppCategory.STUDY -> "学习"
                        else -> "其他"
                    }
                    val limitText = if (app.limitMs > 0) " · 限额 ${app.limitMs / 60_000}分" else " · 点击设限额"
                    Text(
                        text = tag + limitText,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = { onSetCategory(app) }) { Text("分类") }
                TextButton(onClick = { onSetLimit(app) }) { Text("限额") }
                Switch(checked = app.locked, onCheckedChange = { onToggleLock(app) })
            }
        }
    }
}

@Composable
private fun WindowList(
    windows: List<BlockedTimeWindowEntity>,
    onDelete: (Long) -> Unit
) {
    if (windows.isEmpty()) {
        Text(
            text = stringResource(R.string.empty),
            modifier = Modifier.padding(24.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(windows, key = { it.id }) { w ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = w.label.ifBlank { "时段" },
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "${TimeUtil.minuteToHm(w.startMinute)} - ${TimeUtil.minuteToHm(w.endMinute)}" +
                            if (w.appliesToAllApps) " · 全部应用" else " · 指定应用",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { onDelete(w.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete))
                }
            }
        }
    }
}

@Composable
private fun LimitDialog(
    currentMinutes: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    var text by remember { mutableStateOf(if (currentMinutes > 0) currentMinutes.toString() else "") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置每日时长上限（分钟）", fontFamily = FontFamily.Monospace) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter { c -> c.isDigit() }.take(4) },
                label = { Text("分钟（0 = 取消限额）") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text.toIntOrNull() ?: 0) }) {
                Text(stringResource(R.string.confirm))
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun AddWindowDialog(
    apps: List<AppControlItem>,
    onConfirm: (
        label: String,
        startMinute: Int,
        endMinute: Int,
        daysOfWeek: Int,
        appliesToAll: Boolean,
        packages: List<String>
    ) -> Unit,
    onDismiss: () -> Unit
) {
    var label by remember { mutableStateOf("") }
    var start by remember { mutableStateOf("22:00") }
    var end by remember { mutableStateOf("06:00") }
    var appliesToAll by remember { mutableStateOf(true) }
    var everyday by remember { mutableStateOf(true) }
    var selectedDays by remember { mutableStateOf(setOf(1, 2, 3, 4, 5, 6, 7)) }
    var selectedPackages by remember { mutableStateOf(emptySet<String>()) }
    var timeError by remember { mutableStateOf<String?>(null) }
    val weekDays = listOf(
        1 to "周一", 2 to "周二", 3 to "周三", 4 to "周四",
        5 to "周五", 6 to "周六", 7 to "周日"
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加禁用时段", fontFamily = FontFamily.Monospace) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.verticalScroll(rememberScrollState())
            ) {
                OutlinedTextField(value = label, onValueChange = { label = it }, label = { Text("名称（可选）") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = start, onValueChange = { start = it; timeError = null }, label = { Text("开始 HH:mm") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = end, onValueChange = { end = it; timeError = null }, label = { Text("结束 HH:mm") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                timeError?.let {
                    Text(it, color = NeonMagenta, style = MaterialTheme.typography.labelSmall, fontFamily = FontFamily.Monospace)
                }

                Text("星期", style = MaterialTheme.typography.labelMedium, fontFamily = FontFamily.Monospace)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = everyday, onCheckedChange = { everyday = it })
                    Text("每天")
                }
                if (!everyday) {
                    weekDays.forEach { (day, name) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = day in selectedDays,
                                onCheckedChange = { checked ->
                                    selectedDays = if (checked) selectedDays + day else selectedDays - day
                                }
                            )
                            Text(name)
                        }
                    }
                }

                Text("应用范围", style = MaterialTheme.typography.labelMedium, fontFamily = FontFamily.Monospace)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = appliesToAll, onClick = { appliesToAll = true })
                    Text("全部应用")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = !appliesToAll, onClick = { appliesToAll = false })
                    Text("指定应用")
                }
                if (!appliesToAll) {
                    if (apps.isEmpty()) {
                        Text("暂无可用应用", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        apps.forEach { app ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(
                                    checked = app.packageName in selectedPackages,
                                    onCheckedChange = { checked ->
                                        selectedPackages = if (checked) selectedPackages + app.packageName else selectedPackages - app.packageName
                                    }
                                )
                                Text(app.label)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val s = parseHm(start)
                    val e = parseHm(end)
                    if (s == null || e == null) {
                        timeError = "时间格式无效，请使用 HH:mm（小时 0-23，分钟 0-59）"
                        return@TextButton
                    }
                    val daysMask = if (everyday) 0x7F else selectedDays.fold(0) { acc, d -> acc or (if (d == 7) 1 else (1 shl d)) }
                    if (daysMask == 0) {
                        timeError = "请至少选择一个星期"
                        return@TextButton
                    }
                    val pkgs = if (appliesToAll) emptyList() else selectedPackages.toList()
                    onConfirm(label.trim(), s, e, daysMask, appliesToAll, pkgs)
                }
            ) { Text(stringResource(R.string.confirm)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

@Composable
private fun CategoryDialog(
    currentCategory: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改应用分类", fontFamily = FontFamily.Monospace) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                listOf(
                    AppCategory.GAME to "游戏",
                    AppCategory.STUDY to "学习",
                    AppCategory.OTHER to "其他"
                ).forEach { (category, name) ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = currentCategory == category, onClick = { onConfirm(category) })
                        Text(name)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } }
    )
}

private fun parseHm(text: String): Int? {
    val parts = text.split(":")
    if (parts.size != 2) return null
    val h = parts[0].toIntOrNull() ?: return null
    val m = parts[1].toIntOrNull() ?: return null
    if (h !in 0..23 || m !in 0..59) return null
    return h * 60 + m
}
