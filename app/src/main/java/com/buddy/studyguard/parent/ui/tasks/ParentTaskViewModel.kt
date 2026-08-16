package com.buddy.studyguard.parent.ui.tasks

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buddy.studyguard.common.cloud.CloudBaseManager
import com.buddy.studyguard.common.cloud.CloudSyncRepository
import com.buddy.studyguard.common.cloud.PollingListener
import com.buddy.studyguard.common.data.db.dao.TaskDao
import com.buddy.studyguard.common.data.db.entity.TaskEntity
import com.buddy.studyguard.common.data.db.entity.TaskSource
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ParentTaskViewModel @Inject constructor(
    private val taskDao: TaskDao,
    private val cloudSyncRepository: CloudSyncRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ParentTaskViewModel"
    }

    private var taskListener: PollingListener? = null

    val tasks: StateFlow<List<TaskEntity>> = taskDao.observeBySource(TaskSource.PARENT)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch { syncFromCloud() }
    }

    fun createTask(title: String, description: String, subject: String, dueAt: Long?) {
        if (title.isBlank()) return
        viewModelScope.launch {
            val localId = taskDao.insert(
                TaskEntity(
                    title = title.trim(),
                    description = description.trim(),
                    subject = subject.trim(),
                    source = TaskSource.PARENT,
                    dueAt = dueAt
                )
            )
            // 上报云端并回填 cloudId，弟弟端 TaskBoardViewModel 即可拉取/监听
            launch {
                cloudSyncRepository.createTask(title.trim(), description.trim(), dueAt)
                    .onSuccess { cloudId ->
                        taskDao.setCloudId(localId, cloudId)
                    }
                    .onFailure { Log.w(TAG, "云端同步任务失败: ${it.message}") }
            }
        }
    }

    fun deleteTask(id: Long) = viewModelScope.launch {
        val task = taskDao.getById(id) ?: return@launch
        taskDao.deleteById(id)
        val cloudId = task.cloudId
        if (cloudId != null) {
            launch {
                cloudSyncRepository.deleteTask(cloudId)
                    .onFailure { Log.w(TAG, "云端删除任务失败: ${it.message}") }
            }
        }
    }

    private suspend fun syncFromCloud() {
        val currentUid = CloudBaseManager.currentUserId()
        cloudSyncRepository.getFamilyId()
            .onSuccess { familyId ->
                cloudSyncRepository.fetchTasks(familyId)
                    .onSuccess { docs ->
                        for (doc in docs) {
                            mergeCloudTask(doc, currentUid)
                        }
                    }
                    .onFailure { Log.w(TAG, "拉取云端任务失败: ${it.message}") }

                // 监听云端任务变更，感知弟弟端的完成状态与删除
                taskListener = cloudSyncRepository.listenTasks(
                    familyId,
                    viewModelScope,
                    onChange = { doc ->
                        viewModelScope.launch {
                            mergeCloudTask(doc, CloudBaseManager.currentUserId())
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

    private suspend fun mergeCloudTask(doc: Map<String, Any?>, currentUid: String?) {
        try {
            val idValue = doc["id"]
            val cloudId = when (idValue) {
                is Number -> idValue.toLong().toString()
                is String -> idValue
                else -> null
            } ?: return
            // 家长端只同步自己布置的任务（uid 等于当前登录家长 uid）
            val ownerUid = doc["uid"] as? String
            if (ownerUid != null && currentUid != null && ownerUid != currentUid) return

            val existing = taskDao.getByCloudId(cloudId)
            if (existing != null) {
                val completed = doc["completed"] as? Boolean ?: false
                if (completed != existing.completed) {
                    val now = if (completed) System.currentTimeMillis() else null
                    taskDao.setCompleted(existing.id, completed, now)
                }
            } else {
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
            Log.w(TAG, "合并云端任务失败: ${e.message}")
        }
    }

    override fun onCleared() {
        taskListener?.cancel()
        super.onCleared()
    }
}
