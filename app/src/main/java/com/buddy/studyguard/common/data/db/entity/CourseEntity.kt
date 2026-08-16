package com.buddy.studyguard.common.data.db.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * 周课程表：按「星期几 + 节次」定位。
 *
 * @param dayOfWeek 1=周一 … 7=周日
 * @param period 第几节课（1 起）
 * @param startMinute 开始分钟数（0-1439）
 * @param endMinute 结束分钟数
 */
@Entity(
    tableName = "courses",
    primaryKeys = ["dayOfWeek", "period"],
    indices = [Index("dayOfWeek")]
)
data class CourseEntity(
    val dayOfWeek: Int,
    val period: Int,
    val subject: String,
    val startMinute: Int,
    val endMinute: Int,
    val note: String = ""
)
