// NetworkModule.kt
package com.example.bookapp.data.di

import android.util.Log
import com.example.bookapp.BuildConfig
import com.example.bookapp.data.api.BookService
import com.example.bookapp.domain.error.AppError
import com.example.bookapp.util.Logger
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

object NetworkModule {
    private const val TAG = "NetworkModule"
    private const val BASE_URL = "https://www.googleapis.com/books/v1/"

    init {
        // Add detailed initialization logging
        Log.d(TAG, "Initializing NetworkModule")
        Log.d(TAG, "BuildConfig.DEBUG_MODE: ${BuildConfig.DEBUG_MODE}")
        Log.d(TAG, "BuildConfig.BOOKS_API_KEY: ${BuildConfig.BOOKS_API_KEY}")
    }

    private val moshi = try {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    } catch (e: Exception) {
        Log.e(TAG, "Moshi initialization failed", e)
        throw AppError.Network.ServerError(message = "Failed to initialize JSON parser", cause = e)
    }

    private val networkInterceptor = Interceptor { chain ->
        val request = chain.request()
        val response = chain.proceed(request)

        if (!response.isSuccessful) {
            Logger.e(TAG, "Request failed with code: ${response.code}")
            Log.e(TAG, "Failed request URL: ${request.url}")
        }

        response
    }

    private val apiKeyInterceptor = Interceptor { chain ->
        val original = chain.request()

        // Add more robust API key logging and validation
        val apiKey = BuildConfig.BOOKS_API_KEY
        Log.d(TAG, "Using API Key: $apiKey")

        if (apiKey.isBlank()) {
            Log.e(TAG, "CRITICAL: API KEY IS BLANK!")
            throw IllegalStateException("API Key cannot be blank")
        }

        val url = original.url.newBuilder()
            .addQueryParameter("key", apiKey)
            .build()

        chain.proceed(
            original.newBuilder()
                .url(url)
                .build()
        )
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // Use BODY level for more detailed logging in debug
        level = if (BuildConfig.DEBUG_MODE)
            HttpLoggingInterceptor.Level.BODY
        else
            HttpLoggingInterceptor.Level.BASIC
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(networkInterceptor)
        .addInterceptor(apiKeyInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val retrofit = try {
        Log.d(TAG, "Creating Retrofit instance")
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    } catch (e: Exception) {
        Log.e(TAG, "Retrofit initialization failed", e)
        when (e) {
            is UnknownHostException -> throw AppError.Network.NoConnection(cause = e)
            else -> throw AppError.Network.ServerError(
                message = "Failed to initialize network service: ${e.message}",
                cause = e
            )
        }
    }

    val bookService: BookService = try {
        Log.d(TAG, "Creating BookService")
        retrofit.create(BookService::class.java)
    } catch (e: Exception) {
        Log.e(TAG, "BookService creation failed", e)
        throw AppError.Network.ServiceInitializationError(
            message = "Failed to initialize book service: ${e.message}",
            serviceName = "BookService",
            cause = e
        )
    }
}