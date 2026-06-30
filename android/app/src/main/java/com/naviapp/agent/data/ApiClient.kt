package com.naviapp.agent.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Singleton API client that supports dynamic base URL changes.
 * Recreates the Retrofit instance when the URL is updated.
 */
object ApiClient {

    // Default: EC2 backend
    private var baseUrl: String = "http://3.87.198.126:8000/"
    private var api: NaviApiService? = null

    private val okHttpClient: OkHttpClient by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }
        OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()
    }

    fun getApi(): NaviApiService {
        if (api == null) {
            api = createApi()
        }
        return api!!
    }

    fun updateBaseUrl(url: String) {
        val normalized = if (url.endsWith("/")) url else "$url/"
        if (normalized != baseUrl) {
            baseUrl = normalized
            api = createApi()
        }
    }

    fun getBaseUrl(): String = baseUrl.removeSuffix("/")

    private fun createApi(): NaviApiService {
        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(NaviApiService::class.java)
    }
}
