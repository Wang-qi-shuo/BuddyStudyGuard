package com.buddy.studyguard.common.cloud

import android.content.Context
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * CloudBase（腾讯云开发）单例管理器（HTTP API 版本）。
 *
 * - 环境 ID：[buddystudyguard-d0fakpi02641743d]
 * - 在 [BuddyStudyGuardApp.onCreate] 中调用 [init] 完成初始化。
 */
object CloudBaseManager {

    const val ENV_ID = "buddystudyguard-d0fakpi02641743d"
    @JvmField val BASE_URL = "https://$ENV_ID.api.tcloudbasegateway.com"

    // ── 集合名称常量 ──
    const val COLL_USERS = "users"
    const val COLL_FAMILY_GROUPS = "family_groups"
    const val COLL_MESSAGES = "messages"
    const val COLL_TASKS = "tasks"
    const val COLL_APP_USAGE = "app_usage"
    const val COLL_APP_LOCK_RULES = "app_lock_rules"
    const val COLL_APP_LIMIT_RULES = "app_limit_rules"
    const val COLL_BLOCKED_TIME_WINDOWS = "blocked_time_windows"
    const val COLL_CHILD_APPS = "child_apps"

    private var appContext: Context? = null
    private var accessToken: String? = null
    private var refreshToken: String? = null
    private var currentUid: String? = null
    private var currentUsername: String? = null
    private var tokenExpireAt: Long = 0L
    private val refreshMutex = Mutex()

    private const val PREFS_NAME = "cloudbase_prefs"
    private const val KEY_DEVICE_ID = "device_id"
    private const val KEY_AUTH_ACCESS_TOKEN = "auth_access_token"
    private const val KEY_AUTH_REFRESH_TOKEN = "auth_refresh_token"
    private const val KEY_AUTH_UID = "auth_uid"
    private const val KEY_AUTH_USERNAME = "auth_username"
    private const val KEY_AUTH_EXPIRE_AT = "auth_expire_at"

    val api: CloudBaseApi by lazy { CloudBaseApiService.createApi() }

    fun getAccessToken(): String? = accessToken

    fun setAuth(
        token: String,
        uid: String,
        username: String? = null,
        refreshToken: String? = null,
        expiresIn: Int? = null
    ) {
        accessToken = token
        currentUid = uid
        currentUsername = username
        this.refreshToken = refreshToken
        tokenExpireAt = if (expiresIn != null) System.currentTimeMillis() + expiresIn * 1000L else 0L
        persistAuth()
    }

    fun logout() {
        accessToken = null
        refreshToken = null
        currentUid = null
        currentUsername = null
        tokenExpireAt = 0L
        appContext?.let { ctx ->
            ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
                .remove(KEY_AUTH_ACCESS_TOKEN)
                .remove(KEY_AUTH_REFRESH_TOKEN)
                .remove(KEY_AUTH_UID)
                .remove(KEY_AUTH_USERNAME)
                .remove(KEY_AUTH_EXPIRE_AT)
                .apply()
        }
        CloudSyncRepository.clearAllCaches()
    }

    /** 在 Application onCreate 中调用，保存应用上下文并恢复登录态。 */
    fun init(context: Context) {
        if (appContext != null) return
        appContext = context.applicationContext
        restoreAuth()
    }

    /**
     * 获取本机设备标识（x-device-id），用于账号密码登录。
     * 首次生成后缓存到 SharedPreferences，后续复用。
     */
    fun getDeviceId(): String {
        val ctx = appContext ?: throw IllegalStateException("CloudBaseManager 未初始化")
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var id = prefs.getString(KEY_DEVICE_ID, null)
        if (id.isNullOrEmpty()) {
            id = java.util.UUID.randomUUID().toString()
            prefs.edit().putString(KEY_DEVICE_ID, id).apply()
        }
        return id
    }

    /** 获取当前登录用户 ID，未登录返回 null。 */
    fun currentUserId(): String? = currentUid

    /** 获取当前登录账号（用户名），未登录返回 null。 */
    fun currentUsername(): String? = currentUsername

    /** 判断当前是否有已登录用户。 */
    fun isLoggedIn(): Boolean = currentUid != null

    /** 获取 refresh_token，未登录返回 null。 */
    fun getRefreshToken(): String? = refreshToken

    /** 获取 access_token 过期时间（epoch 毫秒），未设置返回 0。 */
    fun getTokenExpireAt(): Long = tokenExpireAt

    /** 刷新成功后更新内存认证信息并持久化。 */
    fun applyTokens(accessToken: String, refreshToken: String, expiresIn: Int, uid: String) {
        this.accessToken = accessToken
        this.refreshToken = refreshToken
        this.currentUid = uid
        this.tokenExpireAt = System.currentTimeMillis() + expiresIn * 1000L
        persistAuth()
    }

    /**
     * 确保当前 access_token 有效：未登录返回 false；未过期直接返回 true；
     * 已过期则通过 refresh_token 静默刷新，返回刷新结果。
     */
    suspend fun ensureValidToken(): Boolean {
        if (currentUid == null) return false
        if (accessToken != null && System.currentTimeMillis() < tokenExpireAt - 60_000L) {
            return true
        }
        return refreshMutex.withLock { refreshAccessToken() }
    }

    /** 使用 refresh_token 换取新的 access_token，成功返回 true。 */
    suspend fun refreshAccessToken(): Boolean {
        val rt = refreshToken ?: return false
        return try {
            val resp = api.refreshToken(getDeviceId(), RefreshTokenRequest(refresh_token = rt))
            applyTokens(resp.access_token, resp.refresh_token, resp.expires_in, resp.sub)
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun persistAuth() {
        val ctx = appContext ?: return
        val editor = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit()
        if (accessToken != null) editor.putString(KEY_AUTH_ACCESS_TOKEN, accessToken)
        else editor.remove(KEY_AUTH_ACCESS_TOKEN)
        if (refreshToken != null) editor.putString(KEY_AUTH_REFRESH_TOKEN, refreshToken)
        else editor.remove(KEY_AUTH_REFRESH_TOKEN)
        if (currentUid != null) editor.putString(KEY_AUTH_UID, currentUid)
        else editor.remove(KEY_AUTH_UID)
        if (currentUsername != null) editor.putString(KEY_AUTH_USERNAME, currentUsername)
        else editor.remove(KEY_AUTH_USERNAME)
        editor.putLong(KEY_AUTH_EXPIRE_AT, tokenExpireAt)
        editor.apply()
    }

    private fun restoreAuth() {
        val ctx = appContext ?: return
        val prefs = ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        accessToken = prefs.getString(KEY_AUTH_ACCESS_TOKEN, null)
        refreshToken = prefs.getString(KEY_AUTH_REFRESH_TOKEN, null)
        currentUid = prefs.getString(KEY_AUTH_UID, null)
        currentUsername = prefs.getString(KEY_AUTH_USERNAME, null)
        tokenExpireAt = prefs.getLong(KEY_AUTH_EXPIRE_AT, 0L)
    }
}
