package com.buddy.studyguard.common.data.prefs

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 豆包 API Key 的加密存储器（基于 EncryptedSharedPreferences）。
 *
 * 提供保存、读取、检查、清除 API Key 的方法，供 AI 模块在首次进入时弹窗收集 Key，
 * 以及 AiModule 在构建 OkHttp 鉴权拦截器时动态获取。
 */
@Singleton
class ApiKeyPrefs @Inject constructor(
    @ApplicationContext context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        PREFS_NAME,
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    /** 保存 API Key。 */
    fun saveApiKey(key: String) {
        prefs.edit().putString(KEY_DOUBAO_API_KEY, key).apply()
    }

    /** 读取已保存的 API Key，未保存时返回空字符串。 */
    fun getApiKey(): String = prefs.getString(KEY_DOUBAO_API_KEY, "") ?: ""

    /** 是否已保存有效的 API Key（非空且非空白）。 */
    fun hasApiKey(): Boolean = getApiKey().isNotBlank()

    /** 清除已保存的 API Key。 */
    fun clearApiKey() {
        prefs.edit().remove(KEY_DOUBAO_API_KEY).apply()
    }

    private companion object {
        const val PREFS_NAME = "buddy_study_guard_secure_prefs"
        const val KEY_DOUBAO_API_KEY = "doubao_api_key"
    }
}
