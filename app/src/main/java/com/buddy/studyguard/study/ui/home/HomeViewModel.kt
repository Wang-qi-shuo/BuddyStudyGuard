package com.buddy.studyguard.study.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buddy.studyguard.common.cloud.InstalledAppReporter
import com.buddy.studyguard.common.data.db.dao.CourseDao
import com.buddy.studyguard.common.data.db.dao.ParentMessageDao
import com.buddy.studyguard.common.data.db.dao.TaskDao
import com.buddy.studyguard.common.data.db.entity.CourseEntity
import com.buddy.studyguard.common.data.db.entity.ParentMessageEntity
import com.buddy.studyguard.common.data.db.entity.TaskEntity
import com.buddy.studyguard.common.util.TimeUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val parentMessageDao: ParentMessageDao,
    taskDao: TaskDao,
    courseDao: CourseDao,
    private val installedAppReporter: InstalledAppReporter
) : ViewModel() {

    private val today = TimeUtil.dayOfWeek(System.currentTimeMillis())

    init {
        // 学生模式进入后尽快上报一次应用清单，家长端能较快看到学生应用
        viewModelScope.launch { installedAppReporter.syncIfStudent() }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        parentMessageDao.observeLatestActive(),
        taskDao.observePending(),
        courseDao.observeByDay(today)
    ) { msg, tasks, courses ->
        HomeUiState(
            latestMessage = msg,
            todayTasks = tasks.take(5),
            todayCourses = courses
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeUiState())

    fun dismissMessage(id: Long) = viewModelScope.launch { parentMessageDao.dismiss(id) }
}

data class HomeUiState(
    val latestMessage: ParentMessageEntity? = null,
    val todayTasks: List<TaskEntity> = emptyList(),
    val todayCourses: List<CourseEntity> = emptyList()
)
