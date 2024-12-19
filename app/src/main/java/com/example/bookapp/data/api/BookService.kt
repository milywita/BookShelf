// BookService.kt
/**
 * REST API interface for the Google Books API.
 * Handles remote data fetching for book searches with error handling.
 */
package com.example.bookapp.data.api

import android.util.Log
import com.example.bookapp.data.model.BookResponse
import com.example.bookapp.domain.error.AppError
import retrofit2.HttpException
import retrofit2.http.GET
import retrofit2.http.Query
import java.net.SocketTimeoutException
import java.net.UnknownHostException

interface BookService {
    companion object {
        const val TAG = "BookService" // Changed from private to public
    }

    @GET("volumes")
    @Throws(AppError.Network.NoConnection::class, AppError.Network.ServerError::class)
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("maxResults") maxResults: Int = 40
    ): BookResponse
}

/**
 * Extension function to handle network errors consistently
 */
suspend fun <T> safeApiCall(tag: String = BookService.TAG, call: suspend () -> T): T {
    return try {
        call()
    } catch (e: Exception) {
        Log.e(tag, "API call failed", e)
        throw when (e) {
            is UnknownHostException -> AppError.Network.NoConnection(cause = e)
            is SocketTimeoutException -> AppError.Network.ServerError(
                message = "Request timed out",
                cause = e
            )
            is HttpException -> AppError.Network.ServerError(
                message = "Server returned error ${e.code()}",
                cause = e
            )
            else -> AppError.Network.ServerError(
                message = "Network request failed",
                cause = e
            )
        }
    }
}