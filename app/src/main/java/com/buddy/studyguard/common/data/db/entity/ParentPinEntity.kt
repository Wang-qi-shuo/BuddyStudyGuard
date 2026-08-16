package com.buddy.studyguard.common.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 家长模式进入口令。单行记录（固定 id = 1）。
 *
 * 出于安全考虑，存的是口令的 SHA-256 哈希（加盐），不存明文。
 * 首次启动时由 [com.buddy.studyguard.common.data.AppDatabase] 的回调写入默认口令 "1234" 的哈希。
 */
@Entity(tableName = "parent_pin")
data class ParentPinEntity(
    @PrimaryKey val id: Long = PIN_ROW_ID,
    val pinHash: String,
    val salt: String,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val PIN_ROW_ID = 1L
    }
}
