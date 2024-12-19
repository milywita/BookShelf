// NetworkModule.kt
/**
 * Dependency injection module for networking components.
 * Configures and provides Retrofit client with Moshi JSON parsing.
**/

package com.example.bookapp.data.di

import android.util.Log
import com.example.bookapp.data.api.BookService
import com.example.bookapp.domain.error.AppError
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.net.UnknownHostException

object NetworkModule {
    private const val TAG = "NetworkModule"
    private const val BASE_URL = "https://www.googleapis.com/books/v1/"

    private val moshi = try {
        Log.d(TAG, "Initializing Moshi")
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Moshi", e)
        throw AppError.Network.ServerError(
            message = "Failed to initialize JSON parser",
            cause = e
        )
    }

    private val retrofit = try {
        Log.d(TAG, "Initializing Retrofit with base URL: $BASE_URL")
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    } catch (e: Exception) {
        Log.e(TAG, "Failed to initialize Retrofit", e)
        when (e) {
            is UnknownHostException -> throw AppError.Network.NoConnection(cause = e)
            else -> throw AppError.Network.ServerError(
                message = "Failed to initialize network service",
                cause = e
            )
        }
    }

    val bookService: BookService = try {
        Log.d(TAG, "Creating BookService")
        retrofit.create(BookService::class.java)
    } catch (e: Exception) {
        Log.e(TAG, "Failed to create BookService", e)
        throw AppError.Network.ServiceInitializationError(
            message = "Failed to initialize book service",
            serviceName = "BookService",
            cause = e
        )
    }
}