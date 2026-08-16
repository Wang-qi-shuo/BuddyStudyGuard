package com.buddy.studyguard.ai.di

import com.buddy.studyguard.BuildConfig
import com.buddy.studyguard.ai.data.remote.DoubaoApi
import com.buddy.studyguard.common.data.prefs.ApiKeyPrefs
import com.buddy.studyguard.common.util.Constants
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * AI 模块 Hilt 提供者：OkHttpClient（从 [ApiKeyPrefs] 动态读取 API Key + 日志）、Retrofit、[DoubaoApi]。
 */
@Module
@InstallIn(SingletonComponent::class)
object AiModule {

    /**
     * 提供 OkHttpClient：
     * - 鉴权拦截器：从 [ApiKeyPrefs] 动态读取 API Key（每次请求实时读取，支持运行时更换 Key），
     *   附加 `Authorization: Bearer <key>`；Key 为空时用空串（调用会 401，由 Repository 的 Error
     *   分支处理）。
     * - 日志拦截器：仅 debug 构建打印 BODY 级日志，release 关闭。
     * - 超时：30s。
     */
    @Provides
    @Singleton
    fun provideOkHttpClient(apiKeyPrefs: ApiKeyPrefs): OkHttpClient {
        val authInterceptor = Interceptor { chain ->
            val apiKey = apiKeyPrefs.getApiKey()
            val request = chain.request().newBuilder()
                .addHeader("Authorization", "Bearer $apiKey")
                .addHeader("Content-Type", "application/json")
                .build()
            chain.proceed(request)
        }

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.NONE
        }

        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    /**
     * 提供 Retrofit：baseUrl = [Constants.DOUBAO_BASE_URL]，使用 Gson 转换器。
     */
    @Provides
    @Singleton
    fun provideRetrofit(client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(Constants.DOUBAO_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    /**
     * 提供 [DoubaoApi]。
     */
    @Provides
    @Singleton
    fun provideDoubaoApi(retrofit: Retrofit): DoubaoApi =
        retrofit.create(DoubaoApi::class.java)
}
