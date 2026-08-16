package com.buddy.studyguard.ai.data.remote

import com.google.gson.annotations.SerializedName

/**
 * 豆包（火山方舟 Ark）OpenAI 兼容 chat completions 请求体。
 *
 * @param model 模型 ID，默认取 [com.buddy.studyguard.common.util.Constants.DOUBAO_DEFAULT_MODEL]
 * @param messages 多轮对话上下文，按顺序包含 system / user / assistant
 * @param temperature 采样温度，越高越发散；默认 0.7
 * @param stream 是否流式返回；本模块仅使用非流式（false）
 * @param max_tokens 单次回复最大 token 数；null 表示由服务端控制
 */
data class DoubaoRequest(
    val model: String,
    val messages: List<DoubaoMessage>,
    val temperature: Double = 0.7,
    val stream: Boolean = false,
    val max_tokens: Int? = null
)

/**
 * 一条对话消息。
 *
 * @param role 取值 "system" / "user" / "assistant"
 * @param content 文本内容（[String]），或视觉模型的多模态内容（[List]<[DoubaoContentPart]>）
 */
data class DoubaoMessage(
    val role: String,
    val content: Any
)

/**
 * 多模态内容片段（用于视觉模型）。
 *
 * - text 片段：type="text"，携带 [text]
 * - 图片片段：type="image_url"，携带 [imageUrl]（url 为 data:image/...;base64,... 数据 URI）
 */
data class DoubaoContentPart(
    val type: String,
    val text: String? = null,
    @SerializedName("image_url")
    val imageUrl: DoubaoImageUrl? = null
)

/**
 * 图片 URL 载荷（OpenAI 兼容 image_url 格式）。
 */
data class DoubaoImageUrl(
    val url: String
)

/**
 * chat completions 响应体。
 */
data class DoubaoResponse(
    val id: String?,
    val choices: List<DoubaoChoice>,
    val usage: DoubaoUsage?
)

/**
 * 响应中的单个候选回复。
 */
data class DoubaoChoice(
    val index: Int,
    val message: DoubaoMessage,
    @SerializedName("finish_reason")
    val finish_reason: String?
)

/**
 * token 用量统计。
 */
data class DoubaoUsage(
    @SerializedName("prompt_tokens")
    val prompt_tokens: Int,
    @SerializedName("completion_tokens")
    val completion_tokens: Int,
    @SerializedName("total_tokens")
    val total_tokens: Int
)
