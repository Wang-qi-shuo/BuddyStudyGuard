package com.buddy.studyguard.common.cloud

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.buddy.studyguard.common.data.db.dao.AppLimitRuleDao
import com.buddy.studyguard.common.data.db.dao.AppLockStateDao
import com.buddy.studyguard.common.data.db.dao.BlockedTimeWindowDao
import com.buddy.studyguard.common.data.db.entity.AppLimitRuleEntity
import com.buddy.studyguard.common.data.db.entity.AppLockStateEntity
import com.buddy.studyguard.common.data.db.entity.BlockedTimeWindowEntity
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * 定期从云端拉取家庭限制快照并覆盖式写入本地 Room 的 Worker。
 *
 * 每 5 分钟执行一次：拉取 app_lock_rules / app_limit_rules /
 * blocked_time_windows 三张表，先 clearAll 再逐条插入。
 */
@HiltWorker
class RestrictionSyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val appLockStateDao: AppLockStateDao,
    private val appLimitRuleDao: AppLimitRuleDao,
    private val blockedTimeWindowDao: BlockedTimeWindowDao,
    private val cloudSyncRepository: CloudSyncRepository
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        private const val TAG = "RestrictionSyncWorker"
        const val WORK_NAME = "restriction_sync"

        /**
         * 空快照确认阈值：云端限制三表连续 N 次为空才清空本地。
         * 既防止"家长端 push 间隙/失败导致云端暂时为空"时误清空本地已有限制，
         * 又保证"家长真正删除所有限制"后弟弟端能解除（云端持续为空，连续 N 次后清空本地）。
         */
        private const val EMPTY_SNAPSHOT_THRESHOLD = 2

        private const val PREFS_NAME = "restriction_sync"
        private const val KEY_EMPTY_SNAPSHOT_COUNT = "empty_snapshot_count"

        /**
         * 调度定期同步任务（每 5 分钟）。
         * 应在 [BuddyStudyGuardApp.onCreate] 中调用。
         */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<RestrictionSyncWorker>(
                5, TimeUnit.MINUTES
            ).build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }

        /**
         * 立即执行一次限制快照同步（登录成功后调用），
         * 避免新限制/删除限制要等 5 分钟轮询才生效。
         */
        fun runNow(context: Context) {
            val request = OneTimeWorkRequestBuilder<RestrictionSyncWorker>().build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }

    override suspend fun doWork(): Result {
        if (!CloudBaseManager.ensureValidToken()) {
            // 未登录/token 失效时不能静默跳过，否则弟弟端永远拉不到最新限制快照，
            // 导致家长新设限制不生效、删除限制后仍被限制。改为 retry 等待下次重试。
            Log.w(TAG, "未登录或 token 失效，限制快照同步稍后重试")
            return Result.retry()
        }

        Log.d(TAG, "开始拉取家庭限制快照……")
        return try {
            val familyCode = cloudSyncRepository.fetchFamilyCode().getOrThrow()
            val snapshot = cloudSyncRepository.pullRestrictionSnapshot(familyCode).getOrThrow()

            // 空快照保护（延迟确认）：云端三表全空时不能立即清空本地，否则家长端 push 间隙/失败
            // 导致云端暂时为空时，学生端 clearAll 会把本地已有限制误清空（家长设限后学生端仍能进入）。
            // 但也不能永远不清空——家长真正删除所有限制后云端会持续为空，旧限制必须解除。
            // 因此采用"连续 N 次空快照才清空本地"：单次/偶发空快照（间隙、瞬时失败）保留本地，
            // 持续为空（家长确实删光）则清空本地，保证删除限制能生效。
            if (snapshot.locks.isEmpty() && snapshot.limits.isEmpty() && snapshot.windows.isEmpty()) {
                val prefs = applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                val emptyCount = prefs.getInt(KEY_EMPTY_SNAPSHOT_COUNT, 0) + 1
                if (emptyCount >= EMPTY_SNAPSHOT_THRESHOLD) {
                    prefs.edit().putInt(KEY_EMPTY_SNAPSHOT_COUNT, 0).apply()
                    // 连续多次确认云端为空，家长确实删除了所有限制，清空本地三表解除限制
                    appLockStateDao.clearAll()
                    appLimitRuleDao.clearAll()
                    blockedTimeWindowDao.clearAll()
                    Log.w(TAG, "云端限制快照连续 $emptyCount 次为空，确认家长已删除所有限制，清空本地")
                } else {
                    prefs.edit().putInt(KEY_EMPTY_SNAPSHOT_COUNT, emptyCount).apply()
                    Log.w(TAG, "云端限制快照为空（第 $emptyCount 次），保留本地已有限制，等待下次确认")
                }
                return Result.success()
            }

            // 拉到非空快照，说明云端有数据，重置空快照计数
            applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putInt(KEY_EMPTY_SNAPSHOT_COUNT, 0).apply()

            // 覆盖式写入：先清空三张表，再逐条插入
            appLockStateDao.clearAll()
            snapshot.locks.forEach { lock ->
                appLockStateDao.upsert(
                    AppLockStateEntity(
                        packageName = lock.packageName,
                        locked = lock.locked,
                        lockedAt = System.currentTimeMillis()
                    )
                )
            }

            appLimitRuleDao.clearAll()
            snapshot.limits.forEach { limit ->
                appLimitRuleDao.upsert(
                    AppLimitRuleEntity(
                        packageName = limit.packageName,
                        dailyLimitMs = limit.dailyLimitMs,
                        enabled = limit.enabled
                    )
                )
            }

            blockedTimeWindowDao.clearAll()
            snapshot.windows.forEach { window ->
                val id = blockedTimeWindowDao.insert(
                    BlockedTimeWindowEntity(
                        label = window.label,
                        startMinute = window.startMinute,
                        endMinute = window.endMinute,
                        daysOfWeek = window.daysOfWeek,
                        appliesToAllApps = window.appliesToAll,
                        enabled = window.enabled
                    )
                )
                if (!window.appliesToAll && window.packages.isNotEmpty()) {
                    blockedTimeWindowDao.replaceApps(id, window.packages)
                }
            }

            Log.d(
                TAG,
                "同步完成：locks=${snapshot.locks.size}, limits=${snapshot.limits.size}, windows=${snapshot.windows.size}"
            )
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "同步家庭限制快照失败: ${e.message}", e)
            Result.retry()
        }
    }
}
