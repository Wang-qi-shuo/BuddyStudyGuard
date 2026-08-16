package com.buddy.studyguard.common.cloud

import android.util.Log
import kotlinx.coroutines.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 云端数据同步仓库（PostgREST REST API 版本）。
 *
 * 所有方法均为 suspend 函数，返回 [Result] 以支持优雅降级。
 * 云端操作失败不会影响本地功能。
 */
@Singleton
class CloudSyncRepository @Inject constructor() {

    // ═══════════════════════════════════════════════
    //  辅助
    // ═══════════════════════════════════════════════

    /**
     * 从 users 表查询当前登录用户的 family_code。
     * 结果会被缓存，调用 [clearAllCaches] 可强制重新查询。
     */
    suspend fun getFamilyId(): Result<String> = runCatching {
        val now = System.currentTimeMillis()
        cachedFamilyId?.let { cached ->
            if (now - cachedFamilyIdAt < FAMILY_ID_CACHE_TTL_MS) {
                return@runCatching cached
            }
        }
        val uid = CloudBaseManager.currentUserId()
            ?: throw IllegalStateException("未登录")
        val rows = CloudBaseManager.api.query(
            CloudBaseManager.COLL_USERS,
            filters = mapOf("uid" to "eq.$uid")
        )
        val familyCode = rows.firstOrNull()?.get("family_code") as? String
            ?: throw IllegalStateException("未绑定家庭")
        cachedFamilyId = familyCode
        cachedFamilyIdAt = now
        familyCode
    }

    companion object {
        private const val TAG = "CloudSyncRepository"

        /** 缓存当前用户的 family_code，避免重复查询。 */
        private var cachedFamilyId: String? = null

        /** 上次写入缓存的时间戳，配合 TTL 实现到期刷新。 */
        private var cachedFamilyIdAt: Long = 0L

        /** family_code 缓存有效期，到期后重新查询云端，保证家庭码变化后可刷新。 */
        private const val FAMILY_ID_CACHE_TTL_MS = 60_000L

        /** 清除所有静态缓存（登录/注销时调用）。 */
        fun clearAllCaches() {
            cachedFamilyId = null
            cachedFamilyIdAt = 0L
        }
    }

    /** 清除实例缓存（委托静态方法）。 */
    fun clearCache() {
        clearAllCaches()
    }

    /**
     * 从 users 表获取当前用户的手机号。
     */
    suspend fun getCurrentPhone(): Result<String> = runCatching {
        val uid = CloudBaseManager.currentUserId()
            ?: throw IllegalStateException("未登录")
        val rows = CloudBaseManager.api.query(
            CloudBaseManager.COLL_USERS,
            filters = mapOf("uid" to "eq.$uid")
        )
        rows.firstOrNull()?.get("phone") as? String ?: ""
    }

    /**
     * 从 users 表获取当前登录用户的身份标识（student / parent）。
     * 用于学生端上报应用清单前的身份判定。
     */
    suspend fun getCurrentIdentity(): Result<String> = runCatching {
        val uid = CloudBaseManager.currentUserId()
            ?: throw IllegalStateException("未登录")
        val rows = CloudBaseManager.api.query(
            CloudBaseManager.COLL_USERS,
            filters = mapOf("uid" to "eq.$uid")
        )
        rows.firstOrNull()?.get("identity") as? String
            ?: throw IllegalStateException("未绑定身份")
    }

    /**
     * 从 users 表获取指定用户的昵称。
     */
    suspend fun getUserNickname(uid: String): Result<String> = runCatching {
        val rows = CloudBaseManager.api.query(
            CloudBaseManager.COLL_USERS,
            filters = mapOf("uid" to "eq.$uid")
        )
        rows.firstOrNull()?.get("nickname") as? String ?: ""
    }

    /** 从 PostgREST 返回行中提取主键 id（bigserial），统一转字符串。 */
    private fun extractId(row: Map<String, Any?>): String? = when (val id = row["id"]) {
        is Number -> id.toLong().toString()
        is String -> id
        else -> null
    }

    // ═══════════════════════════════════════════════
    //  消息同步
    // ═══════════════════════════════════════════════

    /**
     * 发送消息到云端。
     * @param image 图片 base64 字符串，纯文本消息传 null（不写入 image 字段）。
     * @return 云端记录主键 id 的字符串形式
     */
    suspend fun sendMessage(
        content: String,
        senderType: String,
        image: String? = null
    ): Result<String> = runCatching {
        val uid = CloudBaseManager.currentUserId()
            ?: throw IllegalStateException("未登录")
        val familyCode = getFamilyId().getOrThrow()
        val phone = getCurrentPhone().getOrDefault("")
        val senderName = getUserNickname(uid).getOrDefault("")
        val dataMap = mutableMapOf<String, Any?>(
            "uid" to uid,
            "phone" to phone,
            "sender_type" to senderType,
            "sender_name" to senderName,
            "content" to content,
            "timestamp" to System.currentTimeMillis(),
            "family_code" to familyCode
        )
        if (image != null) {
            dataMap["image"] = image
        }
        try {
            val rows = CloudBaseManager.api.insert(
                CloudBaseManager.COLL_MESSAGES,
                body = listOf(dataMap)
            )
            rows.firstOrNull()?.let { extractId(it) } ?: ""
        } catch (e: Exception) {
            if (image != null) {
                // 带 image 字段插入失败：最常见原因是云端 messages 表缺少 image 列
                // （PostgREST 报 column does not exist），被上层误报为"网络错误"。
                // 记录详细日志便于定位，并降级为纯文本重试，保证文字消息能送达。
                Log.w(
                    TAG,
                    "带图片消息插入失败（可能云端缺 image 列）: ${e.javaClass.simpleName}: ${e.message}"
                )
                val textOnly = dataMap.toMutableMap().apply { remove("image") }
                try {
                    val rows = CloudBaseManager.api.insert(
                        CloudBaseManager.COLL_MESSAGES,
                        body = listOf(textOnly)
                    )
                    rows.firstOrNull()?.let { extractId(it) } ?: ""
                    throw IllegalStateException(
                        "图片上传失败：云端 messages 表可能缺少 image 列，" +
                            "请在 CloudBase 控制台执行 ALTER TABLE messages ADD COLUMN IF NOT EXISTS image TEXT，" +
                            "消息已降级为纯文本发送"
                    )
                } catch (e2: Exception) {
                    if (e2 is IllegalStateException) throw e2
                    Log.w(TAG, "降级纯文本插入也失败: ${e2.javaClass.simpleName}: ${e2.message}")
                    throw e
                }
            } else {
                Log.w(TAG, "消息插入失败: ${e.javaClass.simpleName}: ${e.message}")
                throw e
            }
        }
    }

    /**
     * 分页拉取家庭消息（按时间倒序）。
     * @param limit 每页条数，默认 50。
     * @param offset 偏移量，用于加载更早消息。
     */
    suspend fun fetchMessages(
        familyCode: String,
        limit: Int = 50,
        offset: Int = 0
    ): Result<List<Map<String, Any?>>> = runCatching {
        CloudBaseManager.api.query(
            CloudBaseManager.COLL_MESSAGES,
            filters = mapOf("family_code" to "eq.$familyCode"),
            order = "timestamp.desc",
            limit = limit,
            offset = offset
        )
    }

    /**
     * 实时监听家庭消息新增（轮询方式）。
     */
    fun listenMessages(
        familyCode: String,
        scope: CoroutineScope,
        onNewMessage: (Map<String, Any?>) -> Unit
    ): PollingListener {
        val processedIds = mutableSetOf<String>()
        var lastTimestamp = 0L
        val job = scope.launch {
            while (isActive) {
                delay(5000)
                try {
                    val rows = CloudBaseManager.api.query(
                        CloudBaseManager.COLL_MESSAGES,
                        filters = mapOf(
                            "family_code" to "eq.$familyCode",
                            "timestamp" to "gt.$lastTimestamp"
                        ),
                        order = "timestamp.desc",
                        limit = 50
                    )
                    rows.reversed().forEach { doc ->
                        val cloudId = extractId(doc) ?: return@forEach
                        if (processedIds.add(cloudId)) {
                            val ts = (doc["timestamp"] as? Number)?.toLong() ?: 0L
                            if (ts > lastTimestamp) lastTimestamp = ts
                            onNewMessage(doc)
                        }
                    }
                } catch (_: Exception) {
                    // 静默忽略轮询错误，下一轮继续
                }
            }
        }
        return PollingListener(job)
    }

    // ═══════════════════════════════════════════════
    //  任务同步
    // ═══════════════════════════════════════════════

    /**
     * 创建任务到云端。
     * @return 云端记录主键 id 的字符串形式
     */
    suspend fun createTask(
        title: String,
        content: String,
        deadline: Long?
    ): Result<String> = runCatching {
        val uid = CloudBaseManager.currentUserId()
            ?: throw IllegalStateException("未登录")
        val familyCode = getFamilyId().getOrThrow()
        val data = mutableMapOf<String, Any?>(
            "uid" to uid,
            "title" to title,
            "content" to content,
            "completed" to false,
            "family_code" to familyCode,
            "created_at" to System.currentTimeMillis()
        )
        if (deadline != null) {
            data["deadline"] = deadline
        }
        val rows = CloudBaseManager.api.insert(
            CloudBaseManager.COLL_TASKS,
            body = listOf(data)
        )
        rows.firstOrNull()?.let { extractId(it) } ?: ""
    }

    /**
     * 拉取家庭全部任务。
     */
    suspend fun fetchTasks(familyCode: String): Result<List<Map<String, Any?>>> = runCatching {
        CloudBaseManager.api.query(
            CloudBaseManager.COLL_TASKS,
            filters = mapOf("family_code" to "eq.$familyCode")
        )
    }

    /**
     * 标记云端任务为已完成。
     */
    suspend fun markTaskCompleted(taskId: String): Result<Unit> = runCatching {
        CloudBaseManager.api.update(
            CloudBaseManager.COLL_TASKS,
            filters = mapOf("id" to "eq.$taskId"),
            body = mapOf("completed" to true)
        )
    }

    /**
     * 删除云端任务。
     */
    suspend fun deleteTask(taskId: String): Result<Unit> = runCatching {
        CloudBaseManager.api.delete(
            CloudBaseManager.COLL_TASKS,
            filters = mapOf("id" to "eq.$taskId")
        )
    }

    /**
     * 实时监听家庭任务变更（轮询方式）。
     */
    fun listenTasks(
        familyCode: String,
        scope: CoroutineScope,
        onChange: (Map<String, Any?>) -> Unit,
        onDelete: ((String) -> Unit)? = null
    ): PollingListener {
        // cloudId -> completed，用于感知任务完成状态变化（首次出现或完成状态翻转时回调）
        val completedStates = mutableMapOf<String, Boolean>()
        // 上一轮云端存在的 cloudId，用于感知删除（本轮消失则回调 onDelete）
        val knownIds = mutableSetOf<String>()
        val job = scope.launch {
            while (isActive) {
                delay(5000)
                try {
                    val rows = CloudBaseManager.api.query(
                        CloudBaseManager.COLL_TASKS,
                        filters = mapOf("family_code" to "eq.$familyCode")
                    )
                    val currentIds = mutableSetOf<String>()
                    rows.forEach { doc ->
                        val cloudId = extractId(doc) ?: return@forEach
                        currentIds.add(cloudId)
                        val completed = doc["completed"] as? Boolean ?: false
                        val prev = completedStates[cloudId]
                        if (prev == null || prev != completed) {
                            completedStates[cloudId] = completed
                            onChange(doc)
                        }
                    }
                    if (onDelete != null) {
                        val deleted = knownIds - currentIds
                        deleted.forEach { cloudId ->
                            completedStates.remove(cloudId)
                            onDelete(cloudId)
                        }
                    }
                    knownIds.clear()
                    knownIds.addAll(currentIds)
                } catch (_: Exception) {
                    // 静默忽略轮询错误，下一轮继续
                }
            }
        }
        return PollingListener(job)
    }

    // ═══════════════════════════════════════════════
    //  应用时长同步
    // ═══════════════════════════════════════════════

    /**
     * 上报单条应用使用记录到云端。
     */
    suspend fun reportAppUsage(
        appName: String,
        duration: Long,
        date: String,
        packageName: String,
        category: String
    ): Result<String> = runCatching {
        val uid = CloudBaseManager.currentUserId()
            ?: throw IllegalStateException("未登录")
        val familyCode = getFamilyId().getOrThrow()
        // 覆盖式上报：先删除该用户当天同一应用的旧记录，避免多次上报导致家长端 sum 虚高
        CloudBaseManager.api.delete(
            CloudBaseManager.COLL_APP_USAGE,
            filters = mapOf(
                "uid" to "eq.$uid",
                "date" to "eq.$date",
                "package_name" to "eq.$packageName"
            )
        )
        val rows = CloudBaseManager.api.insert(
            CloudBaseManager.COLL_APP_USAGE,
            body = listOf(mapOf(
                "uid" to uid,
                "app_name" to appName,
                "duration" to duration,
                "date" to date,
                "timestamp" to System.currentTimeMillis(),
                "family_code" to familyCode,
                "package_name" to packageName,
                "category" to category
            ))
        )
        rows.firstOrNull()?.let { extractId(it) } ?: ""
    }

    /**
     * 拉取指定日期的应用使用记录。
     * @param uid 可选，传入时仅拉取该学生的记录；传 null 则不过滤 uid。
     */
    suspend fun fetchAppUsage(
        familyCode: String,
        date: String,
        uid: String? = null
    ): Result<List<Map<String, Any?>>> = runCatching {
        val filters = mutableMapOf(
            "family_code" to "eq.$familyCode",
            "date" to "eq.$date"
        )
        if (uid != null) {
            filters["uid"] = "eq.$uid"
        }
        CloudBaseManager.api.query(
            CloudBaseManager.COLL_APP_USAGE,
            filters = filters
        )
    }

    /**
     * 拉取指定日期范围的应用使用汇总数据。
     * @param uid 可选，传入时仅拉取该学生的记录；传 null 则不过滤 uid。
     */
    suspend fun fetchAppUsageSummary(
        familyCode: String,
        fromDate: String,
        toDate: String,
        uid: String? = null
    ): Result<List<Map<String, Any?>>> = runCatching {
        val filters = mutableMapOf(
            "family_code" to "eq.$familyCode",
            "and" to "(date.gte.$fromDate,date.lte.$toDate)"
        )
        if (uid != null) {
            filters["uid"] = "eq.$uid"
        }
        CloudBaseManager.api.query(
            CloudBaseManager.COLL_APP_USAGE,
            filters = filters
        )
    }

    /**
     * 删除指定家庭中 date 早于 cutoffDate 的应用使用记录，用于云端 30 天自动清理。
     */
    suspend fun deleteAppUsageBefore(
        familyCode: String,
        cutoffDate: String
    ): Result<Unit> = runCatching {
        CloudBaseManager.api.delete(
            CloudBaseManager.COLL_APP_USAGE,
            filters = mapOf(
                "family_code" to "eq.$familyCode",
                "date" to "lt.$cutoffDate"
            )
        )
    }

    // ═══════════════════════════════════════════════
    //  已安装应用清单同步（学生上报 / 家长拉取）
    // ═══════════════════════════════════════════════

    /**
     * 上报学生端已安装应用清单（覆盖式：先删该学生旧数据，再全量插入）。
     * 家长端拉取后可与本机应用列表合并，从而限制学生手机独有的应用。
     */
    suspend fun reportInstalledApps(
        familyCode: String,
        apps: List<InstalledAppSnapshot>
    ): Result<Unit> = runCatching {
        val uid = CloudBaseManager.currentUserId()
            ?: throw IllegalStateException("未登录")
        CloudBaseManager.api.delete(
            CloudBaseManager.COLL_CHILD_APPS,
            filters = mapOf("uid" to "eq.$uid")
        )
        if (apps.isNotEmpty()) {
            val now = System.currentTimeMillis()
            CloudBaseManager.api.insert(
                CloudBaseManager.COLL_CHILD_APPS,
                body = apps.map {
                    mapOf(
                        "uid" to uid,
                        "family_code" to familyCode,
                        "package_name" to it.packageName,
                        "app_name" to it.appName,
                        "category" to it.category,
                        "updated_at" to now
                    )
                }
            )
        }
    }

    /**
     * 拉取指定家庭的已安装应用清单（学生端上报）。
     */
    suspend fun fetchChildApps(familyCode: String): Result<List<Map<String, Any?>>> = runCatching {
        CloudBaseManager.api.query(
            CloudBaseManager.COLL_CHILD_APPS,
            filters = mapOf("family_code" to "eq.$familyCode")
        )
    }

    /**
     * 拉取指定学生在云端已安装应用清单（含家长手动分类）。
     * 学生端据此把家长的分类覆盖到本地，保证跨端口径一致。
     */
    suspend fun fetchChildAppsByUid(
        familyCode: String,
        uid: String
    ): Result<List<Map<String, Any?>>> = runCatching {
        CloudBaseManager.api.query(
            CloudBaseManager.COLL_CHILD_APPS,
            filters = mapOf(
                "family_code" to "eq.$familyCode",
                "uid" to "eq.$uid"
            )
        )
    }

    /**
     * 更新云端 child_apps 表中指定学生某应用的分类（家长手动分类下发）。
     */
    suspend fun updateChildAppCategory(
        familyCode: String,
        studentUid: String,
        packageName: String,
        category: String
    ): Result<Unit> = runCatching {
        CloudBaseManager.api.update(
            CloudBaseManager.COLL_CHILD_APPS,
            filters = mapOf(
                "family_code" to "eq.$familyCode",
                "uid" to "eq.$studentUid",
                "package_name" to "eq.$packageName"
            ),
            body = mapOf("category" to category)
        )
    }

    // ═══════════════════════════════════════════════
    //  限制规则快照同步（家长下发 / 学生拉取）
    // ═══════════════════════════════════════════════

    /**
     * 查询当前登录用户的 family_code（不缓存）。
     */
    suspend fun fetchFamilyCode(): Result<String> = runCatching {
        val uid = CloudBaseManager.currentUserId()
            ?: throw IllegalStateException("未登录")
        val rows = CloudBaseManager.api.query(
            CloudBaseManager.COLL_USERS,
            filters = mapOf("uid" to "eq.$uid")
        )
        rows.firstOrNull()?.get("family_code") as? String
            ?: throw IllegalStateException("未绑定家庭")
    }

    /**
     * 查询指定家庭码下学生的 uid。
     */
    suspend fun fetchStudentUid(familyCode: String): Result<String> = runCatching {
        val rows = CloudBaseManager.api.query(
            CloudBaseManager.COLL_USERS,
            filters = mapOf(
                "family_code" to "eq.$familyCode",
                "identity" to "eq.student"
            )
        )
        rows.firstOrNull()?.get("uid") as? String
            ?: throw IllegalStateException("未找到学生账号")
    }

    /**
     * 上报本地限制规则快照到云端（覆盖式：先删该家庭旧数据，再全量插入）。
     */
    suspend fun pushRestrictionSnapshot(
        familyCode: String,
        locks: List<AppLockRuleSnapshot>,
        limits: List<AppLimitRuleSnapshot>,
        windows: List<BlockedTimeWindowSnapshot>
    ): Result<Unit> = runCatching {
        val now = System.currentTimeMillis()
        val familyFilter = mapOf("family_code" to "eq.$familyCode")

        CloudBaseManager.api.delete(CloudBaseManager.COLL_APP_LOCK_RULES, filters = familyFilter)
        CloudBaseManager.api.delete(CloudBaseManager.COLL_APP_LIMIT_RULES, filters = familyFilter)
        CloudBaseManager.api.delete(CloudBaseManager.COLL_BLOCKED_TIME_WINDOWS, filters = familyFilter)

        if (locks.isNotEmpty()) {
            insertWithRetry(
                CloudBaseManager.COLL_APP_LOCK_RULES,
                locks.map {
                    mapOf(
                        "family_code" to familyCode,
                        "package_name" to it.packageName,
                        "app_name" to it.appName,
                        "locked" to it.locked,
                        "updated_at" to now
                    )
                }
            )
        }
        if (limits.isNotEmpty()) {
            insertWithRetry(
                CloudBaseManager.COLL_APP_LIMIT_RULES,
                limits.map {
                    mapOf(
                        "family_code" to familyCode,
                        "package_name" to it.packageName,
                        "app_name" to it.appName,
                        "daily_limit_ms" to it.dailyLimitMs,
                        "enabled" to it.enabled,
                        "updated_at" to now
                    )
                }
            )
        }
        if (windows.isNotEmpty()) {
            insertWithRetry(
                CloudBaseManager.COLL_BLOCKED_TIME_WINDOWS,
                windows.map {
                    mapOf(
                        "family_code" to familyCode,
                        "label" to it.label,
                        "start_minute" to it.startMinute,
                        "end_minute" to it.endMinute,
                        "days_of_week" to it.daysOfWeek,
                        "applies_to_all" to it.appliesToAll,
                        "enabled" to it.enabled,
                        "packages" to it.packages.joinToString(","),
                        "updated_at" to now
                    )
                }
            )
        }
    }

    /**
     * 带重试的批量插入：push 采用"先 delete 再 insert"的覆盖式策略，
     * 若 insert 因网络抖动/瞬时错误失败，云端三表已被清空，学生端会拉到空快照。
     * 这里失败后重试一次，尽量保证云端数据完整，避免"家长设限后学生端不生效"。
     */
    private suspend fun insertWithRetry(table: String, body: List<Map<String, Any?>>) {
        try {
            CloudBaseManager.api.insert(table, body = body)
        } catch (e: Exception) {
            Log.w(TAG, "插入 $table 失败，重试一次: ${e.message}")
            CloudBaseManager.api.insert(table, body = body)
        }
    }

    /**
     * 拉取指定家庭的限制规则快照。
     */
    suspend fun pullRestrictionSnapshot(familyCode: String): Result<RestrictionSnapshot> = runCatching {
        val locks = CloudBaseManager.api.query(
            CloudBaseManager.COLL_APP_LOCK_RULES,
            filters = mapOf("family_code" to "eq.$familyCode")
        ).map { row ->
            AppLockRuleSnapshot(
                packageName = row["package_name"] as? String ?: "",
                appName = row["app_name"] as? String ?: "",
                locked = (row["locked"] as? Boolean) ?: false
            )
        }
        val limits = CloudBaseManager.api.query(
            CloudBaseManager.COLL_APP_LIMIT_RULES,
            filters = mapOf("family_code" to "eq.$familyCode")
        ).map { row ->
            AppLimitRuleSnapshot(
                packageName = row["package_name"] as? String ?: "",
                appName = row["app_name"] as? String ?: "",
                dailyLimitMs = (row["daily_limit_ms"] as? Number)?.toLong() ?: 0L,
                enabled = (row["enabled"] as? Boolean) ?: true
            )
        }
        val windows = CloudBaseManager.api.query(
            CloudBaseManager.COLL_BLOCKED_TIME_WINDOWS,
            filters = mapOf("family_code" to "eq.$familyCode")
        ).map { row ->
            BlockedTimeWindowSnapshot(
                label = row["label"] as? String ?: "",
                startMinute = (row["start_minute"] as? Number)?.toInt() ?: 0,
                endMinute = (row["end_minute"] as? Number)?.toInt() ?: 0,
                daysOfWeek = (row["days_of_week"] as? Number)?.toInt() ?: 0x7F,
                appliesToAll = (row["applies_to_all"] as? Boolean) ?: true,
                enabled = (row["enabled"] as? Boolean) ?: true,
                packages = (row["packages"] as? String)
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?: emptyList()
            )
        }
        RestrictionSnapshot(locks, limits, windows)
    }
}

/** 轮询监听器，通过 [cancel] 停止轮询。 */
class PollingListener(private val job: Job) {
    fun cancel() { job.cancel() }
}

/** 限制规则快照（三张表合并结果）。 */
data class RestrictionSnapshot(
    val locks: List<AppLockRuleSnapshot>,
    val limits: List<AppLimitRuleSnapshot>,
    val windows: List<BlockedTimeWindowSnapshot>
)

/** 应用锁定规则快照。 */
data class AppLockRuleSnapshot(
    val packageName: String,
    val appName: String,
    val locked: Boolean
)

/** 应用时长限制规则快照。 */
data class AppLimitRuleSnapshot(
    val packageName: String,
    val appName: String,
    val dailyLimitMs: Long,
    val enabled: Boolean
)

/** 禁用时段快照。 */
data class BlockedTimeWindowSnapshot(
    val label: String,
    val startMinute: Int,
    val endMinute: Int,
    val daysOfWeek: Int,
    val appliesToAll: Boolean,
    val enabled: Boolean,
    val packages: List<String>
)

/** 已安装应用清单快照（学生端上报）。 */
data class InstalledAppSnapshot(
    val packageName: String,
    val appName: String,
    val category: String
)
