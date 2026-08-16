package com.buddy.studyguard.common.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 家长发给孩子模式的消息。孩子模式首页顶部卡片展示最新一条。
 */
@Entity(tableName = "parent_messages")
data class ParentMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    /** 是否已被孩子在首页关闭（关闭后不再顶部置顶展示，但保留历史）。 */
    val dismissed: Boolean = false,
    /** 是否需要振动提醒（写入时由家长模式触发一次振动）。 */
    val vibrate: Boolean = true
)
