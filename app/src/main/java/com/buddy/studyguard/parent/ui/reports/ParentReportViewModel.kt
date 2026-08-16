package com.buddy.studyguard.parent.ui.reports

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buddy.studyguard.common.cloud.CloudSyncRepository
import com.buddy.studyguard.common.cloud.RestrictionSnapshot
import com.buddy.studyguard.common.data.db.dao.AppCategoryDao
import com.buddy.studyguard.common.data.db.dao.StudySessionDao
import com.buddy.studyguard.common.data.db.dao.SubjectDuration
import com.buddy.studyguard.common.data.db.dao.TaskDao
import com.buddy.studyguard.common.data.db.entity.AppCategory
import com.buddy.studyguard.common.data.db.entity.TaskSource
import com.buddy.studyguard.common.util.TimeUtil
import com.buddy.studyguard.study.ui.stats.StatsRange
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class ParentReportViewModel @Inject constructor(
    private val studySessionDao: StudySessionDao,
    private val cloudSyncRepository: CloudSyncRepository,
    private val appCategoryDao: AppCategoryDao,
    taskDao: TaskDao
) : ViewModel() {

    private val _range = MutableStateFlow(StatsRange.TODAY)
    val range: StateFlow<StatsRange> = _range.asStateFlow()

    private val refreshTrigger = MutableStateFlow(0L)

    init {
        viewModelScope.launch {
            while (true) {
                delay(60_000)
                refreshTrigger.value = System.currentTimeMillis()
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val data: StateFlow<ReportData> = combine(_range, refreshTrigger) { r, _ -> r }
        .flatMapLatest { r ->
            val (start, end) = when (r) {
                StatsRange.TODAY -> TimeUtil.dayRange(System.currentTimeMillis())
                StatsRange.WEEK -> TimeUtil.currentWeekRange()
            }
            combine(
                flow { emit(studySessionDao.sumBySubject(start, end)) },
                taskDao.observeBySource(TaskSource.PARENT),
                fetchCloudUsage(r),
                appCategoryDao.observeAll()
            ) { bySubj, tasks, cloud, categories ->
                val usage = cloud.usage
                val restrictedPackages = cloud.restrictedPackages

                fun rowsDuration(rows: List<Map<String, Any?>>): Long =
                    rows.sumOf { (it["duration"] as? Number)?.toLong() ?: 0L }

                fun rowsRestricted(rows: List<Map<String, Any?>>): Boolean =
                    rows.mapNotNull { it["package_name"] as? String }
                        .any { it in restrictedPackages }

                // 以家长手动分类为准；无本地分类记录时回退到云端上报分类
                val catMap = categories.associateBy { it.packageName }
                val gameRows = usage.filter { row ->
                    val pkg = row["package_name"] as? String
                    val local = pkg?.let { catMap[it] }
                    if (local != null) local.category == AppCategory.GAME
                    else row["category"] == AppCategory.GAME
                }
                val gameMs = gameRows.sumOf { (it["duration"] as? Number)?.toLong() ?: 0L }

                val gameDetails = gameRows
                    .groupBy { it["app_name"] as? String ?: "" }
                    .map { (name, rows) -> GameDetail(appName = name, durationMs = rowsDuration(rows)) }
                    .filter { it.appName.isNotEmpty() }
                    .sortedByDescending { it.durationMs }

                val appUsageDetails = usage
                    .groupBy { it["app_name"] as? String ?: "" }
                    .map { (name, rows) ->
                        AppUsageDetail(
                            appName = name,
                            durationMs = rowsDuration(rows),
                            restricted = rowsRestricted(rows)
                        )
                    }
                    .filter { it.appName.isNotEmpty() }
                    .sortedByDescending { it.durationMs }

                val weeklyUsage = usage
                    .groupBy { it["date"] as? String ?: "" }
                    .map { (date, rows) ->
                        val parsed = runCatching { LocalDate.parse(date) }.getOrNull()
                        val label = parsed?.let { DAILY_LABELS[it.dayOfWeek.value - 1] } ?: date
                        DailyUsage(date = date, label = label, durationMs = rowsDuration(rows))
                    }
                    .filter { it.date.isNotEmpty() }
                    .sortedBy { it.date }

                ReportData(
                    studyBySubject = bySubj,
                    gameMs = gameMs,
                    completedTasks = tasks.count { it.completed },
                    totalTasks = tasks.size,
                    gameDetails = gameDetails,
                    appUsageDetails = appUsageDetails,
                    weeklyUsage = weeklyUsage
                )
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReportData())

    fun setRange(r: StatsRange) { _range.value = r }

    private data class CloudUsageData(
        val usage: List<Map<String, Any?>>,
        val restrictedPackages: Set<String>
    )

    private fun fetchCloudUsage(r: StatsRange) = flow {
        val data = withContext(Dispatchers.IO) {
            try {
                val familyCode = cloudSyncRepository.fetchFamilyCode().getOrThrow()
                val studentUid = cloudSyncRepository.fetchStudentUid(familyCode).getOrThrow()
                val usage = when (r) {
                    StatsRange.TODAY -> {
                        val today = TimeUtil.todayDayString()
                        cloudSyncRepository.fetchAppUsage(familyCode, today, studentUid)
                            .getOrDefault(emptyList())
                    }
                    StatsRange.WEEK -> {
                        val (start, end) = TimeUtil.currentWeekRange()
                        val from = TimeUtil.dayString(start)
                        val to = TimeUtil.dayString(end - 1)
                        cloudSyncRepository.fetchAppUsageSummary(familyCode, from, to, studentUid)
                            .getOrDefault(emptyList())
                    }
                }
                val snapshot = cloudSyncRepository.pullRestrictionSnapshot(familyCode)
                    .getOrDefault(RestrictionSnapshot(emptyList(), emptyList(), emptyList()))
                val restrictedPackages = (
                    snapshot.locks.filter { it.locked }.map { it.packageName } +
                        snapshot.limits.filter { it.enabled }.map { it.packageName }
                    ).toSet()
                CloudUsageData(usage, restrictedPackages)
            } catch (_: Exception) {
                CloudUsageData(emptyList(), emptySet())
            }
        }
        emit(data)
    }

    companion object {
        private val DAILY_LABELS = listOf("周一", "周二", "周三", "周四", "周五", "周六", "周日")
    }
}

data class ReportData(
    val studyBySubject: List<SubjectDuration> = emptyList(),
    val gameMs: Long = 0,
    val completedTasks: Int = 0,
    val totalTasks: Int = 0,
    val gameDetails: List<GameDetail> = emptyList(),
    val appUsageDetails: List<AppUsageDetail> = emptyList(),
    val weeklyUsage: List<DailyUsage> = emptyList()
)

data class GameDetail(
    val appName: String,
    val durationMs: Long
)

data class AppUsageDetail(
    val appName: String,
    val durationMs: Long,
    val restricted: Boolean = false
)

data class DailyUsage(
    val date: String,
    val label: String,
    val durationMs: Long
)
