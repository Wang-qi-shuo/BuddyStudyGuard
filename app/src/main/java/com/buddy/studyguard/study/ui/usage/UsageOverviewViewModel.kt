package com.buddy.studyguard.study.ui.usage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buddy.studyguard.common.data.db.dao.AppCategoryDao
import com.buddy.studyguard.common.data.db.dao.AppUsageRecordDao
import com.buddy.studyguard.common.data.db.entity.AppCategory
import com.buddy.studyguard.common.data.db.entity.AppCategoryEntity
import com.buddy.studyguard.common.data.db.entity.AppUsageRecordEntity
import com.buddy.studyguard.common.util.TimeUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class UsageOverviewViewModel @Inject constructor(
    private val appUsageRecordDao: AppUsageRecordDao,
    appCategoryDao: AppCategoryDao
) : ViewModel() {

    private val today = TimeUtil.todayDayString()

    val uiState: StateFlow<UsageUiState> = combine(
        appUsageRecordDao.observeByDay(today),
        appCategoryDao.observeAll()
    ) { records, categories ->
        val catMap = categories.associateBy { it.packageName }
        val gamePkgs = categories.filter { it.category == AppCategory.GAME }.map { it.packageName }
        val gameMs = if (gamePkgs.isEmpty()) 0L
            else appUsageRecordDao.sumForegroundMsForPackages(today, gamePkgs)
        UsageUiState(records, catMap, gameMs)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UsageUiState())
}

data class UsageUiState(
    val records: List<AppUsageRecordEntity> = emptyList(),
    val categoryMap: Map<String, AppCategoryEntity> = emptyMap(),
    val gameTotalMs: Long = 0
) {
    val totalMs: Long get() = records.sumOf { it.foregroundMs }
    /** 游戏总时长的格式化字符串，如 "1小时30分钟"。 */
    val formattedGameTime: String get() = TimeUtil.formatGameTime(gameTotalMs)
    /** 今日总时长的格式化字符串。 */
    val formattedTotalTime: String get() = TimeUtil.formatGameTime(totalMs)
}
