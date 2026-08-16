package com.buddy.studyguard.common.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 学习会话（专注计时记录）。
 *
 * @param mode [FocusMode.POMODORO] 番茄钟 / [FocusMode.STOPWATCH] 正计时
 * @param durationMs 实际学习时长（毫秒）
 */
@Entity(tableName = "study_sessions")
data class StudySessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val subject: String,
    val mode: String = FocusMode.STOPWATCH,
    val startAt: Long,
    val durationMs: Long,
    val note: String = ""
)

object FocusMode {
    const val POMODORO = "POMODORO"
    const val STOPWATCH = "STOPWATCH"
}
