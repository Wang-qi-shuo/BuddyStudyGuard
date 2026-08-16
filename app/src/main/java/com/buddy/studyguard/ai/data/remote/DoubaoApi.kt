package com.buddy.studyguard.ai.data.remote

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * 豆包（火山方舟 Ark）OpenAI 兼容 chat completions 接口。
 *
 * baseUrl 由 [com.buddy.studyguard.ai.di.AiModule] 注入为
 * [com.buddy.studyguard.common.util.Constants.DOUBAO_BASE_URL]，
 * 并由 OkHttp 拦截器统一附加 `Authorization: Bearer <key>` 头。
 */
interface DoubaoApi {

    /**
     * 发起一次对话补全。
     *
     * 使用 [Response] 包装返回，便于在 Repository 中手动处理 HTTP 错误码
     * （如 401 未授权、429 限流、5xx 服务异常）。
     */
    @POST("chat/completions")
    suspend fun chat(@Body request: DoubaoRequest): Response<DoubaoResponse>
}
