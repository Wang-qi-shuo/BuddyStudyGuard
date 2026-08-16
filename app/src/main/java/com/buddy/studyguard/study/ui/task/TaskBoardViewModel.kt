package com.buddy.studyguard.study.ui.task

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buddy.studyguard.common.cloud.CloudSyncRepository
import com.buddy.studyguard.common.cloud.PollingListener
import com.buddy.studyguard.common.data.db.dao.TaskDao
import com.buddy.studyguard.common.data.db.entity.TaskEntity
import com.buddy.studyguard.common.data.db.entity.TaskSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TaskBoardViewModel @Inject constructor(
    private val taskDao: TaskDao,
    private val cloudSyncRepository: CloudSyncRepository
) : ViewModel() {

    companion object {
        private const val TAG = "TaskBoardViewModel"
    }

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private var taskListener: PollingListener? = null

    val tasks: StateFlow<List<TaskEntity>> = taskDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        // 从云端拉取任务列表，合并到本地（异步，不影响 UI）
        viewModelScope.launch { syncFromCloud() }
    }

    /**
     * 从云端拉取任务并合并到本地 Room。
     */
    private suspend fun syncFromCloud() {
        cloudSyncRepository.getFamilyId()
            .onSuccess { familyId ->
                cloudSyncRepository.fetchTasks(familyId)
                    .onSuccess { docs ->
                        for (doc in docs) {
                            try {
                                val idValue = doc["id"]
                                val cloudId = when (idValue) {
                                    is Number -> idValue.toLong().toString()
                                    is String -> idValue
                                    else -> null
                                } ?: continue
                                val title = doc["title"] as? String ?: ""
                                val description = doc["content"] as? String ?: ""
                                val deadline = (doc["deadline"] as? Number)?.toLong()
                                val completed = doc["completed"] as? Boolean ?: false
                                val createdAt = (doc["created_at"] as? Number)?.toLong()
                                    ?: System.currentTimeMillis()
                                // 去重：已存在相同 cloudId 则跳过
                                val existing = taskDao.getByCloudId(cloudId)
                                if (existing != null) continue
                                taskDao.insert(
                                    TaskEntity(
                                        title = title,
                                        description = description,
                                        source = TaskSource.PARENT,
                                        createdAt = createdAt,
                                        dueAt = deadline,
                                        completed = completed,
                                        cloudId = cloudId
                                    )
                                )
                            } catch (e: Exception) {
                                Log.w(TAG, "合并云端任务失败: ${e.message}")
                            }
                        }
                    }
                    .onFailure { Log.w(TAG, "拉取云端任务失败: ${it.message}") }

                // 监听云端任务变更（含删除感知）
                taskListener = cloudSyncRepository.listenTasks(
                    familyCode = familyId,
                    scope = viewModelScope,
                    onChange = { doc ->
                        viewModelScope.launch {
                            try {
                                val idValue = doc["id"]
                                val cloudId = when (idValue) {
                                    is Number -> idValue.toLong().toString()
                                    is String -> idValue
                                    else -> null
                                } ?: return@launch
                                val existing = taskDao.getByCloudId(cloudId)
                                if (existing != null) {
                                    // 已存在：同步完成状态
                                    val completed = doc["completed"] as? Boolean ?: false
                                    if (completed != existing.completed) {
                                        val now = if (completed) System.currentTimeMillis() else null
                                        taskDao.setCompleted(existing.id, completed, now)
                                    }
                                } else {
                                    // 新任务：插入本地
                                    val title = doc["title"] as? String ?: ""
                                    val description = doc["content"] as? String ?: ""
                                    val deadline = (doc["deadline"] as? Number)?.toLong()
                                    val completed = doc["completed"] as? Boolean ?: false
                                    val createdAt = (doc["created_at"] as? Number)?.toLong()
                                        ?: System.currentTimeMillis()
                                    taskDao.insert(
                                        TaskEntity(
                                            title = title,
                                            description = description,
                                            source = TaskSource.PARENT,
                                            createdAt = createdAt,
                                            dueAt = deadline,
                                            completed = completed,
                                            cloudId = cloudId
                                        )
                                    )
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "监听任务变更处理失败: ${e.message}")
                            }
                        }
                    },
                    onDelete = { cloudId ->
                        viewModelScope.launch {
                            taskDao.deleteByCloudId(cloudId)
                        }
                    }
                )
            }
            .onFailure { Log.w(TAG, "获取 familyId 失败，跳过云端任务同步: ${it.message}") }
    }

    fun addTask(title: String, description: String, subject: String, dueAt: Long?) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val localId = taskDao.insert(
                TaskEntity(
                    title = title.trim(),
                    description = description.trim(),
                    subject = subject.trim(),
                    source = TaskSource.CHILD,
                    dueAt = dueAt
                )
            )
            // 同步到云端（异步，失败不影响本地）
            launch {
                cloudSyncRepository.createTask(title.trim(), description.trim(), dueAt)
                    .onSuccess { cloudId ->
                        taskDao.setCloudId(localId, cloudId)
                    }
                    .onFailure { Log.w(TAG, "云端同步任务失败: ${it.message}") }
            }
        }
    }

    fun toggleComplete(task: TaskEntity) {
        viewModelScope.launch {
            val now = if (!task.completed) System.currentTimeMillis() else null
            taskDao.setCompleted(task.id, !task.completed, now)
            // 同步到云端
            val cloudId = task.cloudId
            if (cloudId != null) {
                launch {
                    cloudSyncRepository.markTaskCompleted(cloudId)
                        .onFailure { Log.w(TAG, "云端标记任务完成失败: ${it.message}") }
                }
            }
        }
    }

    fun deleteTask(id: Long) {
        viewModelScope.launch {
            val task = taskDao.getById(id) ?: return@launch
            if (task.source == TaskSource.PARENT) {
                _snackbarMessage.value = "家长布置的任务无法删除"
                return@launch
            }
            taskDao.deleteById(id)
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    override fun onCleared() {
        taskListener?.cancel()
        super.onCleared()
    }
}
