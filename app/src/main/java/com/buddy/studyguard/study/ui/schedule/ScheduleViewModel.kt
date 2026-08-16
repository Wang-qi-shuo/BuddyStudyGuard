package com.buddy.studyguard.study.ui.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buddy.studyguard.common.data.db.dao.CourseDao
import com.buddy.studyguard.common.data.db.entity.CourseEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ScheduleViewModel @Inject constructor(
    private val courseDao: CourseDao
) : ViewModel() {

    val courses: StateFlow<List<CourseEntity>> = courseDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun upsert(course: CourseEntity) = viewModelScope.launch { courseDao.upsert(course) }

    fun delete(dayOfWeek: Int, period: Int) =
        viewModelScope.launch { courseDao.delete(dayOfWeek, period) }
}
