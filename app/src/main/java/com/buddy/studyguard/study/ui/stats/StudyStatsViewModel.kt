package com.buddy.studyguard.study.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buddy.studyguard.common.data.db.dao.StudySessionDao
import com.buddy.studyguard.common.data.db.dao.SubjectDuration
import com.buddy.studyguard.common.util.TimeUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class StudyStatsViewModel @Inject constructor(
    private val studySessionDao: StudySessionDao
) : ViewModel() {

    private val _range = MutableStateFlow(StatsRange.TODAY)
    val range: StateFlow<StatsRange> = _range.asStateFlow()

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val data: StateFlow<StatsData> = _range.flatMapLatest { r ->
        val (start, end) = when (r) {
            StatsRange.TODAY -> TimeUtil.dayRange(System.currentTimeMillis())
            StatsRange.WEEK -> TimeUtil.currentWeekRange()
        }
        flow {
            val total = studySessionDao.sumDurationBetween(start, end)
            val bySubj = studySessionDao.sumBySubject(start, end)
            emit(StatsData(total, bySubj))
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StatsData())

    fun setRange(r: StatsRange) { _range.value = r }
}

enum class StatsRange { TODAY, WEEK }

data class StatsData(
    val totalMs: Long = 0,
    val bySubject: List<SubjectDuration> = emptyList()
)
