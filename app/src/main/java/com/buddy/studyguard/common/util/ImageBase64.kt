package com.buddy.studyguard.common.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream

/**
 * 图片编码工具：将本地图片 Uri 读取为 base64 数据，供视觉模型调用。
 */
object ImageBase64 {

    /** 压缩后最长边像素，避免 base64 过大导致请求体超限。 */
    private const val MAX_EDGE = 1024

    /** JPEG 压缩质量。 */
    private const val JPEG_QUALITY = 75

    /**
     * 读取 [uri] 指向的图片内容，压缩后返回 (base64, mimeType)。
     *
     * 压缩策略与聊天图片一致：最长边 1024、JPEG 质量 75。
     * 失败（如 Uri 无效、内容为空、权限异常）时返回 null，调用方应降级为纯文本或提示用户。
     */
    fun encode(context: Context, uri: Uri): Pair<String, String>? = try {
        val mime = context.contentResolver.getType(uri) ?: "image/jpeg"
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        if (bytes == null || bytes.isEmpty()) {
            null
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sample = 1
            while (bounds.outWidth / sample > MAX_EDGE || bounds.outHeight / sample > MAX_EDGE) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
            if (bitmap == null) {
                // 解码失败时退回原图 base64
                Base64.encodeToString(bytes, Base64.NO_WRAP) to mime
            } else {
                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
                bitmap.recycle()
                Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP) to "image/jpeg"
            }
        }
    } catch (e: Exception) {
        null
    }
}
