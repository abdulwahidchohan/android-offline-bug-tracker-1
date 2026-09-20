package com.uopeople.cs4405.bugtracker.data.remote

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Retrofit client configuration.
 * Contains the centralized base URL placeholder and HTTP client setup.
 */
object RetrofitClient {

    /**
     * Centralized Base URL for remote synchronization.
     * In an academic environment without a live deployed server, this placeholder
     * marks the endpoint location. Replace this with your staging or local mock server URL.
     * (e.g., http://10.0.2.2:8080/v1/ when testing against a local host server from an Android emulator).
     */
    const val BASE_URL = "https://api.bugtracker.uopeople.internal/v1/"

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    val issueApi: IssueApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(IssueApi::class.java)
    }
}
