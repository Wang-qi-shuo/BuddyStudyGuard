package com.buddy.studyguard.common.cloud

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * 聊天图片 base64 编解码工具。
 *
 * CloudBase 无对象存储上传能力，聊天图片采用 base64 方案：
 * - 发送方：压缩（最长边 1024、JPEG 质量 75）后编码为 base64，写入 messages.image 字段。
 * - 接收方：解码 base64 落盘到本地缓存目录，返回 file:// URI 供 Coil 展示。
 */
object ImageBase64 {

    /** 压缩并编码为 base64，失败返回 null。 */
    fun compressAndEncode(context: Context, uri: Uri): String? {
        return try {
            val bytes = context.contentResolver.openInputStream(uri)
                ?.use { it.readBytes() }
                ?: return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sample = 1
            while (bounds.outWidth / sample > 1024 || bounds.outHeight / sample > 1024) {
                sample *= 2
            }
            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, opts)
                ?: return null
            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 75, out)
            bitmap.recycle()
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    /** 解码 base64 落盘到本地缓存目录，返回 file:// URI，失败返回 null。 */
    fun decodeAndSave(context: Context, base64: String, fileName: String): String? {
        return try {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            val dir = File(context.cacheDir, "chat_images").apply { mkdirs() }
            val file = File(dir, fileName)
            file.writeBytes(bytes)
            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 保存聊天图片到系统相册（MediaStore.Images）。
     * 返回 null 表示成功，否则返回错误提示文案。
     *
     * - Android 10+（API 29+）：MediaStore RELATIVE_PATH 直接写入，无需存储权限。
     * - Android 9 及以下（API 26-28）：需 WRITE_EXTERNAL_STORAGE 权限，调用方须先申请。
     *
     * [imageUri] 支持 file://（接收方解码落盘的缓存文件）与 content://（本地相册/相机 URI）。
     */
    fun saveToGallery(context: Context, imageUri: String): String? {
        return try {
            val bytes = readImageBytes(context, imageUri) ?: return "读取图片失败"
            val resolver = context.contentResolver
            val displayName = "chat_${System.currentTimeMillis()}.jpg"
            val mimeType = "image/jpeg"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, displayName)
                put(MediaStore.Images.Media.MIME_TYPE, mimeType)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    put(
                        MediaStore.Images.Media.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + "/BuddyStudyGuard"
                    )
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                } else {
                    val dir = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        "BuddyStudyGuard"
                    ).apply { mkdirs() }
                    put(MediaStore.Images.Media.DATA, File(dir, displayName).absolutePath)
                }
            }
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            val uri = resolver.insert(collection, values) ?: return "无法创建相册条目"
            resolver.openOutputStream(uri)?.use { it.write(bytes) } ?: run {
                resolver.delete(uri, null, null)
                return "无法写入相册"
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            null
        } catch (e: Exception) {
            "保存失败：${e.message}"
        }
    }

    /** 读取图片字节，兼容 file:// 与 content:// URI。 */
    private fun readImageBytes(context: Context, imageUri: String): ByteArray? {
        return try {
            val uri = Uri.parse(imageUri)
            if (uri.scheme == "file") {
                File(uri.path ?: return null).readBytes()
            } else {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
        } catch (e: Exception) {
            null
        }
    }
}
