package com.buddy.studyguard.common.util

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/**
 * 家长口令哈希工具：SHA-256 加盐。
 * 数据库只存哈希与盐，不存明文。默认口令 "123456" 由 [com.buddy.studyguard.common.data.db.AppDatabase.SEED_CALLBACK] 写入。
 */
object PinHasher {

    const val DEFAULT_PIN = "123456"

    /** 生成 16 字节随机盐，Base64 编码返回。 */
    fun newSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return Base64.getEncoder().encodeToString(bytes)
    }

    /** 对口令加盐后做 SHA-256，返回十六进制字符串。 */
    fun hash(pin: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt.toByteArray(Charsets.UTF_8))
        val digest = md.digest(pin.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    /** 校验口令是否匹配。 */
    fun verify(pin: String, expectedHash: String, salt: String): Boolean =
        hash(pin, salt).equals(expectedHash, ignoreCase = true)
}
