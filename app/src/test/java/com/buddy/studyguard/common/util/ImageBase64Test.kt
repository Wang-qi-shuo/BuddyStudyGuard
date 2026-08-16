package com.buddy.studyguard.common.util

import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Base64
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

/**
 * ImageBase64 单元测试：依赖 Android 框架（Bitmap/Base64），使用 Robolectric 在 JVM 上运行。
 * 覆盖正常压缩编码、空输入、无效字节回退、异常降级等路径。
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ImageBase64Test {

    /** 用 Robolectric 的 Bitmap 生成一张真实 PNG 字节，供 BitmapFactory 解码。 */
    private fun createPngBytes(width: Int = 200, height: Int = 200): ByteArray {
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        return out.toByteArray()
    }

    private fun mockContext(bytes: ByteArray?, mime: String = "image/png"): Context {
        val resolver = mockk<ContentResolver>()
        every { resolver.getType(any()) } returns mime
        every { resolver.openInputStream(any()) } returns (bytes?.let { ByteArrayInputStream(it) })
        val context = mockk<Context>()
        every { context.contentResolver } returns resolver
        return context
    }

    private val uri = Uri.parse("content://test/1")

    @Test
    fun encode_validImage_returnsJpegBase64() {
        val context = mockContext(createPngBytes())
        val result = ImageBase64.encode(context, uri)
        assertNotNull(result)
        val (b64, mime) = result!!
        // 压缩后统一输出 JPEG
        assertEquals("image/jpeg", mime)
        val decoded = Base64.decode(b64, Base64.NO_WRAP)
        assertTrue(decoded.isNotEmpty())
    }

    @Test
    fun encode_largeImage_downscaledToMaxEdge() {
        // 3000x2000 图片，最长边 3000 > 1024，应被采样压缩
        val context = mockContext(createPngBytes(3000, 2000))
        val result = ImageBase64.encode(context, uri)
        assertNotNull(result)
        val (b64, mime) = result!!
        assertEquals("image/jpeg", mime)
        val decoded = Base64.decode(b64, Base64.NO_WRAP)
        assertTrue(decoded.isNotEmpty())
    }

    @Test
    fun encode_emptyBytes_returnsNull() {
        val context = mockContext(ByteArray(0))
        assertNull(ImageBase64.encode(context, uri))
    }

    @Test
    fun encode_nullStream_returnsNull() {
        val context = mockContext(null)
        assertNull(ImageBase64.encode(context, uri))
    }

    // 注：Robolectric 的 BitmapFactory 不校验字节有效性，对任意字节均返回非空 Bitmap，
    // 因此"解码失败退回原图 base64"分支无法在 JVM 上模拟，此处不覆盖该分支。

    @Test
    fun encode_resolverThrows_returnsNull() {
        val resolver = mockk<ContentResolver>()
        every { resolver.getType(any()) } returns "image/png"
        every { resolver.openInputStream(any()) } throws RuntimeException("boom")
        val context = mockk<Context>()
        every { context.contentResolver } returns resolver
        assertNull(ImageBase64.encode(context, uri))
    }
}
