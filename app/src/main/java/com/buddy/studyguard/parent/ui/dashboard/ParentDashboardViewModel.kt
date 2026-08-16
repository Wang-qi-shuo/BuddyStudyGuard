package com.buddy.studyguard.parent.ui.dashboard

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buddy.studyguard.common.cloud.CloudSyncRepository
import com.buddy.studyguard.common.data.db.dao.AppCategoryDao
import com.buddy.studyguard.common.data.db.dao.AppUsageRecordDao
import com.buddy.studyguard.common.data.db.dao.StudySessionDao
import com.buddy.studyguard.common.data.db.entity.AppCategory
import com.buddy.studyguard.common.data.db.entity.AppCategoryEntity
import com.buddy.studyguard.common.data.db.entity.AppUsageRecordEntity
import com.buddy.studyguard.common.util.AppClassifier
import com.buddy.studyguard.common.util.TimeUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ParentDashboardViewModel @Inject constructor(
    private val appUsageRecordDao: AppUsageRecordDao,
    private val appCategoryDao: AppCategoryDao,
    private val studySessionDao: StudySessionDao,
    private val cloudSyncRepository: CloudSyncRepository
) : ViewModel() {

    companion object {
        private const val TAG = "ParentDashboardVM"
    }

    private val today = TimeUtil.todayDayString()
    private val todayRange = TimeUtil.dayRange(System.currentTimeMillis())
    private val weekRange = TimeUtil.currentWeekRange()

    val uiState: StateFlow<DashboardUiState> = combine(
        appUsageRecordDao.observeByDay(today),
        appCategoryDao.observeAll(),
        flow {
            val todayStudy = studySessionDao.sumDurationBetween(todayRange.first, todayRange.second)
            val weekStudy = studySessionDao.sumDurationBetween(weekRange.first, weekRange.second)
            emit(todayStudy to weekStudy)
        }
    ) { records, categories, studyPair ->
        val catMap = categories.associateBy { it.packageName }
        val gamePkgs = categories.filter { it.category == AppCategory.GAME }.map { it.packageName }
        val gameMs = if (gamePkgs.isEmpty()) 0L
            else appUsageRecordDao.sumForegroundMsForPackages(today, gamePkgs)
        DashboardUiState(
            topApps = records.take(5),
            categoryMap = catMap,
            todayGameMs = gameMs,
            todayStudyMs = studyPair.first,
            weekStudyMs = studyPair.second
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardUiState())

    init {
        // 从云端拉取今日应用使用数据，缓存到本地 Room
        viewModelScope.launch { syncUsageFromCloud() }
    }

    /**
     * 从云端拉取今日的应用使用记录，写入本地 Room。
     * 本地 Flow 会自动感知 Room 变更并刷新 UI。
     */
    private suspend fun syncUsageFromCloud() {
        cloudSyncRepository.getFamilyId()
            .onSuccess { familyId ->
                cloudSyncRepository.fetchAppUsage(familyId, today)
                    .onSuccess { docs ->
                        for (doc in docs) {
                            try {
                                val pkg = doc["package_name"] as? String ?: continue
                                val appName = doc["app_name"] as? String ?: pkg
                                val duration = (doc["duration"] as? Number)?.toLong() ?: 0L
                                val date = doc["date"] as? String ?: today
                                val category = doc["category"] as? String
                                    ?: AppClassifier.classify(pkg, appName)
                                appUsageRecordDao.upsert(
                                    AppUsageRecordEntity(
                                        packageName = pkg,
                                        day = date,
                                        foregroundMs = duration,
                                        launchCount = 0
                                    )
                                )
                                // 确保本地存在 包名→应用名/分类 映射，
                                // 供首页排行展示应用名及今日游戏时长汇总使用
                                if (appCategoryDao.get(pkg) == null) {
                                    appCategoryDao.upsert(
                                        AppCategoryEntity(
                                            packageName = pkg,
                                            label = appName,
                                            category = category,
                                            customOverride = false
                                        )
                                    )
                                }
                            } catch (e: Exception) {
                                Log.w(TAG, "合并云端使用数据失败: ${e.message}")
                            }
                        }
                    }
                    .onFailure { Log.w(TAG, "拉取云端使用数据失败: ${it.message}") }
            }
            .onFailure { Log.w(TAG, "获取 familyId 失败，跳过云端使用数据同步: ${it.message}") }
    }

    /** 手动刷新：重新从云端拉取数据。 */
    fun refresh() {
        viewModelScope.launch { syncUsageFromCloud() }
    }
}

data class DashboardUiState(
    val topApps: List<AppUsageRecordEntity> = emptyList(),
    val categoryMap: Map<String, AppCategoryEntity> = emptyMap(),
    val todayGameMs: Long = 0,
    val todayStudyMs: Long = 0,
    val weekStudyMs: Long = 0
)
