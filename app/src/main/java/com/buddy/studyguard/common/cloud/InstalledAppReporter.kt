package com.buddy.studyguard.common.cloud

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.buddy.studyguard.common.data.db.dao.AppCategoryDao
import com.buddy.studyguard.common.data.db.entity.AppCategoryEntity
import com.buddy.studyguard.common.util.AppClassifier
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 学生端已安装应用清单上报器。
 *
 * 统一封装「仅学生身份上报」的判定与采集逻辑，供：
 * - [UsageReportWorker] 周期上报
 * - 学生模式启动时的一次性快速上报
 * 复用，保证家长账号不检测、不上报。
 */
@Singleton
class InstalledAppReporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val appCategoryDao: AppCategoryDao,
    private val cloudSyncRepository: CloudSyncRepository
) {
    companion object {
        private const val TAG = "InstalledAppReporter"
        const val IDENTITY_STUDENT = "student"
    }

    /**
     * 仅当当前账号为学生身份时，采集本机已安装应用并覆盖式上报到云端。
     * @return 是否执行了上报
     */
    suspend fun syncIfStudent(): Boolean {
        if (!CloudBaseManager.ensureValidToken()) {
            Log.w(TAG, "登录态失效，跳过上报")
            return false
        }
        val identity = cloudSyncRepository.getCurrentIdentity().getOrNull()
        if (identity != IDENTITY_STUDENT) {
            Log.d(TAG, "当前账号身份为 $identity，跳过应用清单上报")
            return false
        }
        val familyCode = cloudSyncRepository.fetchFamilyCode().getOrNull() ?: run {
            Log.w(TAG, "未获取到 family_code，跳过应用清单上报")
            return false
        }
        // 先拉取家长端下发的分类覆盖本地，避免上报时用旧分类回写覆盖家长设置
        pullParentCategories(familyCode)
        val apps = collectInstalledApps()
        cloudSyncRepository.reportInstalledApps(familyCode, apps)
            .onSuccess { Log.d(TAG, "已上报 ${apps.size} 个已安装应用") }
            .onFailure { e -> Log.w(TAG, "上报已安装应用清单失败: ${e.message}") }
        return true
    }

    /** 拉取家长端下发的应用分类并覆盖本地（学生端消费点）。 */
    private suspend fun pullParentCategories(familyCode: String) {
        val uid = CloudBaseManager.currentUserId() ?: return
        cloudSyncRepository.fetchChildAppsByUid(familyCode, uid)
            .onSuccess { docs ->
                val entities = docs.mapNotNull { doc ->
                    val pkg = doc["package_name"] as? String ?: return@mapNotNull null
                    val label = doc["app_name"] as? String ?: pkg
                    val category = doc["category"] as? String ?: return@mapNotNull null
                    AppCategoryEntity(pkg, label, category, customOverride = true)
                }
                if (entities.isNotEmpty()) appCategoryDao.upsertAll(entities)
                Log.d(TAG, "已拉取家长分类 ${entities.size} 条")
            }
            .onFailure { e -> Log.w(TAG, "拉取家长分类失败: ${e.message}") }
    }

    /** 收集本机启动器可启动的应用清单（排除自身）。 */
    private suspend fun collectInstalledApps(): List<InstalledAppSnapshot> {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val myPkg = context.packageName
        val seen = mutableSetOf<String>()
        val result = mutableListOf<InstalledAppSnapshot>()
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(launcherIntent, 0).forEach { ri ->
            val pkg = ri.activityInfo?.packageName ?: return@forEach
            if (pkg == myPkg || !seen.add(pkg)) return@forEach
            val label = resolveAppLabel(pkg)
            val category = appCategoryDao.get(pkg)?.category
                ?: AppClassifier.classify(pkg, label)
            result.add(InstalledAppSnapshot(pkg, label, category))
        }
        return result
    }

    private fun resolveAppLabel(pkg: String): String = try {
        val pm = context.packageManager
        val ai = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.getApplicationInfo(pkg, PackageManager.ApplicationInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            pm.getApplicationInfo(pkg, 0)
        }
        pm.getApplicationLabel(ai).toString()
    } catch (e: Exception) {
        pkg
    }
}
