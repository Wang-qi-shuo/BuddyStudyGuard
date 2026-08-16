package com.buddy.studyguard.common.data.db.entity

import androidx.room.Entity
import androidx.room.Index

/**
 * AI 对话历史（仅本会话内/跨会话缓存，用于多轮上下文与离线回看）。
 *
 * @param role [AiRole.SYSTEM] / [AiRole.USER] / [AiRole.ASSISTANT]
 * @param sessionId 会话 ID（同一会话内拼接上下文）
 */
@Entity(
    tableName = "ai_messages",
    primaryKeys = ["id"],
    indices = [Index("sessionId"), Index("createdAt")]
)
data class AiMessageEntity(
    val id: Long = 0,
    val sessionId: String,
    val role: String,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    /** 该条消息是否来自离线 FAQ 兜底（用于 UI 区分展示）。 */
    val fromOfflineCache: Boolean = false
) {
    object Ids {
        /** 自增主键需要单独列；这里用 autoGenerate 语义，通过 DAO 返回 id。 */
        const val AUTO = 0L
    }
}

object AiRole {
    const val SYSTEM = "SYSTEM"
    const val USER = "USER"
    const val ASSISTANT = "ASSISTANT"
}
