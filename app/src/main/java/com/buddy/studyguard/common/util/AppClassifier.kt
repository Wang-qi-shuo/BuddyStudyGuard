package com.buddy.studyguard.common.util

import com.buddy.studyguard.common.data.db.entity.AppCategory
import com.buddy.studyguard.common.data.db.entity.AppCategoryEntity

/**
 * 应用分类器：按包名关键字匹配，自动识别游戏 / 学习 / 其他。
 *
 * 家长可在应用控制页手动覆盖分类（写入 [AppCategoryEntity] 且 customOverride=true）。
 * 自动识别仅作为默认值，不覆盖已有的手动分类。
 */
object AppClassifier {

    /** 游戏类包名关键字（小写匹配）。 */
    private val GAME_KEYWORDS = listOf(
        "game", "play", "joy", "arcade", "mmo", "rpg", "shooter",
        "tencent.tmgp", "com.miHoYo", "netease.game", "games",
        "garena", "mobilelegends", "pubg", "codm", "honor", "king"
    )

    /** 知名游戏包名片段。 */
    private val GAME_PACKAGES = listOf(
        "com.tencent.tmgp",
        "com.netease.game",
        "com.miHoYo.",
        "com.mobile.legends",
        "com.garena.game",
        "com.supercell.",
        "com.riotgames.",
        "com.kiloo.subwaysurf",
        "com.rovio.",
        "com.mojang."
    )

    /** 学习类包名关键字。 */
    private val STUDY_KEYWORDS = listOf(
        "study", "learn", "edu", "course", "homework", "math",
        "dictionary", "word", "english", "quizlet", "khan", "duolingo",
        "xsteach", "youdao", "baike"
    )

    fun classify(packageName: String, appLabel: String = ""): String {
        val pkg = packageName.lowercase()
        val label = appLabel.lowercase()
        if (GAME_PACKAGES.any { pkg.startsWith(it.lowercase()) } ||
            GAME_KEYWORDS.any { pkg.contains(it) || label.contains(it) }
        ) {
            return AppCategory.GAME
        }
        if (STUDY_KEYWORDS.any { pkg.contains(it) || label.contains(it) }) {
            return AppCategory.STUDY
        }
        return AppCategory.OTHER
    }

    /** 是否为游戏类。 */
    fun isGame(category: String): Boolean = category == AppCategory.GAME

    /** 默认分类实体（自动识别，非手动覆盖）。 */
    fun autoEntity(packageName: String, label: String): AppCategoryEntity =
        AppCategoryEntity(
            packageName = packageName,
            label = label,
            category = classify(packageName, label),
            customOverride = false
        )
}
