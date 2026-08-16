package com.buddy.studyguard.common.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 应用分类：游戏 / 学习 / 其他。
 *
 * 家长可在应用控制页手动调整分类。游戏类应用会在统计与限制中高亮。
 *
 * @param category [AppCategory.GAME] / [AppCategory.STUDY] / [AppCategory.OTHER]
 * @param customOverride 是否为家长手动覆盖（自动识别结果之外）
 */
@Entity(tableName = "app_categories")
data class AppCategoryEntity(
    @PrimaryKey val packageName: String,
    val label: String,
    val category: String = AppCategory.OTHER,
    val customOverride: Boolean = false
)

object AppCategory {
    const val GAME = "GAME"
    const val STUDY = "STUDY"
    const val OTHER = "OTHER"
}
