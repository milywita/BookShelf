// NetworkModule.kt
package com.milywita.bookapp.data.di

import android.util.Log
import com.milywita.bookapp.BuildConfig
import com.milywita.bookapp.data.api.BookService
import com.milywita.bookapp.domain.error.AppError
import com.milywita.bookapp.util.Logger
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
    Log.d(com.milywita.bookapp.data.di.NetworkModule.TAG, "Initializing NetworkModule")
    Log.d(com.milywita.bookapp.data.di.NetworkModule.TAG, "BuildConfig.DEBUG_MODE: ${com.milywita.bookapp.BuildConfig.DEBUG_MODE}")
    Log.d(com.milywita.bookapp.data.di.NetworkModule.TAG, "BuildConfig.BOOKS_API_KEY: ${com.milywita.bookapp.BuildConfig.BOOKS_API_KEY}")
  }

  private val moshi =
      try {
        Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
      } catch (e: Exception) {
        Log.e(com.milywita.bookapp.data.di.NetworkModule.TAG, "Moshi initialization failed", e)
        throw AppError.Network.ServerError(message = "Failed to initialize JSON parser", cause = e)
      }

  private val networkInterceptor = Interceptor { chain ->
    val request = chain.request()
    val response = chain.proceed(request)

    if (!response.isSuccessful) {
      Logger.e(com.milywita.bookapp.data.di.NetworkModule.TAG, "Request failed with code: ${response.code}")
      Log.e(com.milywita.bookapp.data.di.NetworkModule.TAG, "Failed request URL: ${request.url}")
    }

    response
  }

  private val apiKeyInterceptor = Interceptor { chain ->
    val original = chain.request()
    val apiKey = com.milywita.bookapp.BuildConfig.BOOKS_API_KEY

    if (apiKey.isBlank()) {
      Log.e(com.milywita.bookapp.data.di.NetworkModule.TAG, "CRITICAL: API KEY IS BLANK!")
      throw IllegalStateException("API Key cannot be blank")
    }

    val url = original.url.newBuilder().addQueryParameter("key", apiKey).build()

    chain.proceed(original.newBuilder().url(url).build())
  }

  private val loggingInterceptor =
      HttpLoggingInterceptor().apply {
        // Use BODY level for more detailed logging in debug
        level =
            if (com.milywita.bookapp.BuildConfig.DEBUG_MODE) HttpLoggingInterceptor.Level.BODY
            else HttpLoggingInterceptor.Level.BASIC
      }

  private val okHttpClient =
      OkHttpClient.Builder()
          .addInterceptor(com.milywita.bookapp.data.di.RateLimitInterceptor())
          .addInterceptor(com.milywita.bookapp.data.di.NetworkModule.networkInterceptor)
          .addInterceptor(com.milywita.bookapp.data.di.NetworkModule.apiKeyInterceptor)
          .connectTimeout(30, TimeUnit.SECONDS)
          .readTimeout(30, TimeUnit.SECONDS)
          .writeTimeout(30, TimeUnit.SECONDS)
          .build()

  private val retrofit =
      try {
        Log.d(com.milywita.bookapp.data.di.NetworkModule.TAG, "Creating Retrofit instance")
        Retrofit.Builder()
            .baseUrl(com.milywita.bookapp.data.di.NetworkModule.BASE_URL)
            .client(com.milywita.bookapp.data.di.NetworkModule.okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(com.milywita.bookapp.data.di.NetworkModule.moshi))
            .build()
      } catch (e: Exception) {
        Log.e(com.milywita.bookapp.data.di.NetworkModule.TAG, "Retrofit initialization failed", e)
        when (e) {
          is UnknownHostException -> throw AppError.Network.NoConnection(cause = e)
          else ->
              throw AppError.Network.ServerError(
                  message = "Failed to initialize network service: ${e.message}", cause = e)
        }
      }

  val bookService: BookService =
      try {
        Log.d(com.milywita.bookapp.data.di.NetworkModule.TAG, "Creating BookService")
        com.milywita.bookapp.data.di.NetworkModule.retrofit.create(BookService::class.java)
      } catch (e: Exception) {
        Log.e(com.milywita.bookapp.data.di.NetworkModule.TAG, "BookService creation failed", e)
        throw AppError.Network.ServiceInitializationError(
            message = "Failed to initialize book service: ${e.message}",
            serviceName = "BookService",
            cause = e)
      }
}

class RateLimitInterceptor(
    private val requestsPerMinute: Int = 90,
    private val requestsPerDay: Int = 950
) : Interceptor {
  private val requestTimestamps = mutableListOf<Long>()

  @Synchronized
  override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
    val now = System.currentTimeMillis()
    cleanOldTimestamps(now)

    if (requestTimestamps.size >= requestsPerDay) {
      throw AppError.Network.ServerError(message = "Daily API limit reached")
    }

    val minuteAgo = now - TimeUnit.MINUTES.toMillis(1)
    val recentRequests = requestTimestamps.count { it > minuteAgo }
    if (recentRequests >= requestsPerMinute) {
      throw AppError.Network.ServerError(message = "Rate limit exceeded")
    }

    requestTimestamps.add(now)
    return chain.proceed(chain.request())
  }

  private fun cleanOldTimestamps(now: Long) {
    val dayAgo = now - TimeUnit.DAYS.toMillis(1)
    requestTimestamps.removeAll { it < dayAgo }
  }
}
