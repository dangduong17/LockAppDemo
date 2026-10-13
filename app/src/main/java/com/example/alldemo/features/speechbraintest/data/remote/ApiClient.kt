package com.example.alldemo.features.speechbraintest.data.remote

import com.example.alldemo.features.speechbraintest.data.remote.api.GoogleCloudSttApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val DEFAULT_BASE_URL = "https://speech.googleapis.com/"

    val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private fun createOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    fun createSttApi(baseUrl: String = DEFAULT_BASE_URL): GoogleCloudSttApi {
        val safeBaseUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(safeBaseUrl)
            .client(createOkHttpClient())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GoogleCloudSttApi::class.java)
    }
}
