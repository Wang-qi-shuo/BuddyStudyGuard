package com.buddy.studyguard.common.util

import android.content.Context
import coil.ImageLoader
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 后台静默自动清理管理器。
 *
 * 使用 Kotlin Coroutine 每 10 分钟执行一次轻量清理，不触碰数据库、SharedPreferences、
 * 用户文件及下载目录。完全后台运行，无 UI，无通知。
 *
 * 在 [BuddyStudyGuardApp.onCreate] 中通过 [start] 启动。
 */
@Singleton
class AutoCleanupManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val cleanupIntervalMs = 10 * 60 * 1000L // 10 分钟

    /** 启动周期性清理协程。幂等：多次调用不会创建重复协程。 */
    fun start() {
        scope.launch {
            // 延迟首次清理，避免与应用冷启动争抢资源
            delay(30_000L)
            while (isActive) {
                try {
                    performCleanup()
                } catch (_: Exception) {
                    // 静默吞下所有异常，防止崩溃
                }
                delay(cleanupIntervalMs)
            }
        }
    }

    /** 执行一次完整清理流程。 */
    private fun performCleanup() {
        clearImageCache()
        clearTempCache()
        clearWebViewCache()
        suggestGc()
    }

    /** 清理 Coil 图片缓存。 */
    private fun clearImageCache() {
        try {
            val loader = ImageLoader.Builder(context).build()
            loader.diskCache?.clear()
            loader.diskCache?.fileSystem?.let { fs ->
                val cacheRoot = loader.diskCache?.directory
                if (cacheRoot != null && fs.exists(cacheRoot)) {
                    fs.deleteRecursively(cacheRoot)
                }
            }
        } catch (_: Exception) {
            // ImageLoader 可能尚未初始化
        }
        try {
            val loader = ImageLoader.Builder(context).build()
            loader.memoryCache?.clear()
        } catch (_: Exception) {
        }
    }

    /** 清理应用内部临时缓存目录（cache、临时文件），严格不碰数据库/SP/用户文件/下载目录。 */
    private fun clearTempCache() {
        try {
            val cacheDir = context.cacheDir
            if (cacheDir.exists()) {
                cacheDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        try { file.delete() } catch (_: Exception) {}
                    }
                }
            }
        } catch (_: Exception) {}

        try {
            val extCacheDir = context.externalCacheDir
            if (extCacheDir?.exists() == true) {
                extCacheDir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        try { file.delete() } catch (_: Exception) {}
                    }
                }
            }
        } catch (_: Exception) {}
    }

    /** 清理 WebView 缓存（如 AI 聊天可能使用 WebView）。 */
    private fun clearWebViewCache() {
        try {
            // 仅在确认目录存在且为缓存目录时清理
            val webviewDir = File(context.cacheDir, "WebView")
            if (webviewDir.exists() && webviewDir.isDirectory) {
                webviewDir.deleteRecursively()
            }
        } catch (_: Exception) {}

        try {
            val webviewDb = context.getDatabasePath("webview.db")
            if (webviewDb.exists()) {
                try { webviewDb.delete() } catch (_: Exception) {}
            }
        } catch (_: Exception) {}
    }

    /** 建议 JVM 执行 GC（非强制，由 VM 自行决定）。 */
    private fun suggestGc() {
        try {
            System.runFinalization()
            System.gc()
        } catch (_: Exception) {}
    }
}
