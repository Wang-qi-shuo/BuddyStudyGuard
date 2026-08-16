package com.buddy.studyguard.parent.ui.apps

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.buddy.studyguard.common.cloud.AppLimitRuleSnapshot
import com.buddy.studyguard.common.cloud.AppLockRuleSnapshot
import com.buddy.studyguard.common.cloud.BlockedTimeWindowSnapshot
import com.buddy.studyguard.common.cloud.CloudSyncRepository
import com.buddy.studyguard.common.data.db.dao.AppCategoryDao
import com.buddy.studyguard.common.data.db.dao.AppLimitRuleDao
import com.buddy.studyguard.common.data.db.dao.AppLockStateDao
import com.buddy.studyguard.common.data.db.dao.BlockedTimeWindowDao
import com.buddy.studyguard.common.data.db.entity.AppCategory
import com.buddy.studyguard.common.data.db.entity.AppCategoryEntity
import com.buddy.studyguard.common.data.db.entity.AppLimitRuleEntity
import com.buddy.studyguard.common.data.db.entity.AppLockStateEntity
import com.buddy.studyguard.common.data.db.entity.BlockedTimeWindowEntity
import com.buddy.studyguard.common.util.AppClassifier
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppControlViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appLimitRuleDao: AppLimitRuleDao,
    private val appLockStateDao: AppLockStateDao,
    private val blockedTimeWindowDao: BlockedTimeWindowDao,
    private val appCategoryDao: AppCategoryDao,
    private val cloudSyncRepository: CloudSyncRepository
) : ViewModel() {

    companion object {
        private const val TAG = "AppControlViewModel"
    }

    private val _childApps = MutableStateFlow<List<RemoteAppInfo>>(emptyList())

    /** 云端限制同步失败提示（如 token 失效/网络异常），供 UI 展示。 */
    private val _syncError = MutableStateFlow<String?>(null)
    val syncError: StateFlow<String?> = _syncError.asStateFlow()

    fun clearSyncError() {
        _syncError.value = null
    }

    val apps: StateFlow<List<AppControlItem>> = combine(
        appCategoryDao.observeAll(),
        appLockStateDao.observeAll(),
        appLimitRuleDao.observeAll(),
        _childApps
    ) { categories, locks, rules, childApps ->
        val lockMap = locks.associateBy { it.packageName }
        val ruleMap = rules.associateBy { it.packageName }
        val catMap = categories.associateBy { it.packageName }

        // 仅展示弟弟端上报的云端应用，不检测本机应用
        childApps.map { remote ->
            val cat = catMap[remote.packageName]
            AppControlItem(
                packageName = remote.packageName,
                label = remote.label,
                category = cat?.category ?: remote.category,
                locked = lockMap[remote.packageName]?.locked == true,
                limitMs = ruleMap[remote.packageName]?.dailyLimitMs ?: 0L,
                limitEnabled = ruleMap[remote.packageName]?.enabled == true
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val windows: StateFlow<List<BlockedTimeWindowEntity>> = blockedTimeWindowDao.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshChildApps()
    }

    /** 拉取弟弟端上报的已安装应用清单，作为应用控制列表的唯一来源。 */
    fun refreshChildApps() = viewModelScope.launch {
        cloudSyncRepository.getFamilyId()
            .onSuccess { familyId ->
                cloudSyncRepository.fetchChildApps(familyId)
                    .onSuccess { docs ->
                        _childApps.value = docs.mapNotNull { doc ->
                            val pkg = doc["package_name"] as? String ?: return@mapNotNull null
                            val label = doc["app_name"] as? String ?: pkg
                            val category = doc["category"] as? String
                                ?: AppClassifier.classify(pkg, label)
                            RemoteAppInfo(pkg, label, category)
                        }
                    }
                    .onFailure { /* 云端无数据时静默降级，列表为空 */ }
            }
            .onFailure { /* 未绑定家庭时静默降级 */ }
    }

    fun setLocked(pkg: String, locked: Boolean) = viewModelScope.launch {
        // setLocked 是 UPDATE，首次锁定某应用时表中无该行会导致 0 行更新、锁定静默失效；
        // 这里改为先查再 upsert，保证行一定存在。
        val existing = appLockStateDao.get(pkg)
        appLockStateDao.upsert(
            AppLockStateEntity(
                packageName = pkg,
                locked = locked,
                lockedAt = if (locked) System.currentTimeMillis() else existing?.lockedAt ?: 0L
            )
        )
        syncRestrictionsToCloud()
    }

    /** 设置每日时长上限（分钟）。0 或负数表示删除限制。 */
    fun setLimitMinutes(pkg: String, minutes: Int) = viewModelScope.launch {
        if (minutes <= 0) appLimitRuleDao.delete(pkg)
        else appLimitRuleDao.upsert(
            AppLimitRuleEntity(pkg, minutes * 60_000L, enabled = true)
        )
        syncRestrictionsToCloud()
    }

    fun setCategory(pkg: String, label: String, category: String) = viewModelScope.launch {
        appCategoryDao.upsert(AppCategoryEntity(pkg, label, category, customOverride = true))
        syncCategoryToCloud(pkg, category)
    }

    /**
     * 家长手动分类同步到云端 child_apps.category，
     * 学生端下次拉取后覆盖本地，保证首页与报告页口径一致。
     */
    private suspend fun syncCategoryToCloud(pkg: String, category: String) {
        try {
            val familyCode = cloudSyncRepository.getFamilyId().getOrThrow()
            val studentUid = cloudSyncRepository.fetchStudentUid(familyCode).getOrThrow()
            cloudSyncRepository.updateChildAppCategory(familyCode, studentUid, pkg, category)
        } catch (_: Exception) {
            // 云端同步失败静默降级，不影响本地分类
        }
    }

    fun addWindow(
        label: String,
        startMinute: Int,
        endMinute: Int,
        daysOfWeek: Int,
        appliesToAll: Boolean,
        packages: List<String>
    ) = viewModelScope.launch {
        val id = blockedTimeWindowDao.insert(
            BlockedTimeWindowEntity(
                label = label,
                startMinute = startMinute,
                endMinute = endMinute,
                daysOfWeek = daysOfWeek,
                appliesToAllApps = appliesToAll,
                enabled = true
            )
        )
        if (!appliesToAll && packages.isNotEmpty()) {
            blockedTimeWindowDao.replaceApps(id, packages)
        }
        syncRestrictionsToCloud()
    }

    fun deleteWindow(id: Long) = viewModelScope.launch {
        blockedTimeWindowDao.delete(id)
        syncRestrictionsToCloud()
    }

    /**
     * 读取本地限制规则并覆盖式上报到云端。
     * 在每次限制变更后触发；云端失败时通过 [syncError] 提示用户，避免静默失效。
     */
    fun syncRestrictionsToCloud() = viewModelScope.launch {
        try {
            val familyCode = cloudSyncRepository.getFamilyId().getOrThrow()
            val locks = appLockStateDao.getAllLocked().map {
                AppLockRuleSnapshot(
                    packageName = it.packageName,
                    appName = resolveAppLabel(it.packageName),
                    locked = true
                )
            }
            val limits = appLimitRuleDao.getAllEnabled().map {
                AppLimitRuleSnapshot(
                    packageName = it.packageName,
                    appName = resolveAppLabel(it.packageName),
                    dailyLimitMs = it.dailyLimitMs,
                    enabled = it.enabled
                )
            }
            val windows = blockedTimeWindowDao.getAllEnabled().map { w ->
                BlockedTimeWindowSnapshot(
                    label = w.label,
                    startMinute = w.startMinute,
                    endMinute = w.endMinute,
                    daysOfWeek = w.daysOfWeek,
                    appliesToAll = w.appliesToAllApps,
                    enabled = w.enabled,
                    packages = blockedTimeWindowDao.getAppsOf(w.id)
                )
            }
            cloudSyncRepository.pushRestrictionSnapshot(familyCode, locks, limits, windows)
            _syncError.value = null
        } catch (e: Exception) {
            // 云端同步失败时提示用户，避免"家长端改了但弟弟端不生效"且无感知
            Log.w(TAG, "限制快照同步云端失败: ${e.message}")
            _syncError.value = "限制同步云端失败，弟弟端可能未生效，请检查网络后重试"
        }
    }

    @Suppress("DEPRECATION")
    private fun resolveAppLabel(pkg: String): String = try {
        val ai = context.packageManager.getApplicationInfo(pkg, 0)
        context.packageManager.getApplicationLabel(ai).toString()
    } catch (e: Exception) {
        pkg
    }
}

data class AppControlItem(
    val packageName: String,
    val label: String,
    val category: String,
    val locked: Boolean,
    val limitMs: Long,
    val limitEnabled: Boolean
) {
    val isGame: Boolean get() = category == AppCategory.GAME
}

data class RemoteAppInfo(val packageName: String, val label: String, val category: String)
