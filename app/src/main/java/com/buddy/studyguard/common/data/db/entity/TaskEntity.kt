package com.buddy.studyguard.common.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 任务（孩子自建 + 家长布置，合并显示在孩子任务板）。
 *
 * @param source [TaskSource.CHILD] 孩子自建 / [TaskSource.PARENT] 家长布置
 * @param dueAt 截止时间 epoch 毫秒，可为空（无截止）
 * @param completedAt 完成时间，未完成时为 null
 */
@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val subject: String = "",
    val source: String = TaskSource.CHILD,
    val createdAt: Long = System.currentTimeMillis(),
    val dueAt: Long? = null,
    val completed: Boolean = false,
    val completedAt: Long? = null,
    val cloudId: String? = null
)

object TaskSource {
    const val CHILD = "CHILD"
    const val PARENT = "PARENT"
}
