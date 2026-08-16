package com.buddy.studyguard.common.cloud

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

/**
 * CloudBase HTTP API Retrofit 接口定义（PostgREST 版本）。
 */
interface CloudBaseApi {

    // ═══════════════════════════════════════════════
    //  Auth
    // ═══════════════════════════════════════════════

    @POST("auth/v1/signin")
    suspend fun signIn(
        @Header("x-device-id") deviceId: String,
        @Body body: SignInRequest
    ): SignInResponse

    @POST("auth/v1/token")
    suspend fun refreshToken(
        @Header("x-device-id") deviceId: String,
        @Body body: RefreshTokenRequest
    ): SignInResponse

    // ═══════════════════════════════════════════════
    //  Database - PostgREST
    // ═══════════════════════════════════════════════

    /**
     * 查询表记录，返回裸 JSON 数组 [{...}]。
     * 过滤条件通过 [filters] 传入 PostgREST 操作符，如 "uid" to "eq.xxx"、"timestamp" to "gt.123"。
     */
    @GET("v1/rdb/rest/{table}")
    suspend fun query(
        @Path("table") table: String,
        @Query("select") select: String = "*",
        @QueryMap filters: Map<String, @JvmSuppressWildcards String> = emptyMap(),
        @Query("order") order: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): List<Map<String, Any?>>

    /**
     * 插入表记录（批量）。通过 Prefer: return=representation 返回插入后的完整记录数组。
     */
    @POST("v1/rdb/rest/{table}")
    suspend fun insert(
        @Path("table") table: String,
        @Body body: Any,
        @Header("Prefer") prefer: String = "return=representation"
    ): List<Map<String, Any?>>

    /**
     * 按过滤条件更新表记录，body 为待更新字段对象，返回更新后的记录数组。
     */
    @PATCH("v1/rdb/rest/{table}")
    suspend fun update(
        @Path("table") table: String,
        @QueryMap filters: Map<String, @JvmSuppressWildcards String> = emptyMap(),
        @Body body: Any,
        @Header("Prefer") prefer: String = "return=representation"
    ): List<Map<String, Any?>>

    /**
     * 按过滤条件删除表记录。
     */
    @DELETE("v1/rdb/rest/{table}")
    suspend fun delete(
        @Path("table") table: String,
        @QueryMap filters: Map<String, @JvmSuppressWildcards String> = emptyMap(),
        @Header("Prefer") prefer: String = "return=representation"
    ): List<Map<String, Any?>>
}

// ═══════════════════════════════════════════════
//  Auth 数据类
// ═══════════════════════════════════════════════

data class SignInRequest(
    val username: String,
    val password: String
)

data class SignInResponse(
    val access_token: String,
    val refresh_token: String,
    val expires_in: Int,
    val sub: String,
    val token_type: String
)

data class RefreshTokenRequest(
    val grant_type: String = "refresh_token",
    val refresh_token: String
)

// ═══════════════════════════════════════════════
//  Retrofit 工厂
// ═══════════════════════════════════════════════

object CloudBaseApiService {

    fun createApi(): CloudBaseApi {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val token = CloudBaseManager.getAccessToken()
            val isRefresh = original.url.encodedPath.endsWith("/auth/v1/token")
            val request = if (token != null && !isRefresh) {
                original.newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
            } else {
                original
            }
            chain.proceed(request)
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(CloudBaseManager.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CloudBaseApi::class.java)
    }
}
