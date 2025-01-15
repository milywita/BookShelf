// BookService.kt
package com.example.bookapp.data.api

import com.example.bookapp.data.model.BookResponse
import com.example.bookapp.domain.error.AppError
import com.example.bookapp.util.Logger
import retrofit2.HttpException
import retrofit2.http.GET
import retrofit2.http.Query
import java.net.SocketTimeoutException
import java.net.UnknownHostException

fun interface BookService {
  @GET("volumes")
  @Throws(AppError.Network.NoConnection::class, AppError.Network.ServerError::class)
  suspend fun searchBooks(
      @Query("q") query: String,
      @Query("maxResults") maxResults: Int
  ): BookResponse

  companion object {
    const val TAG = "BookService"
    const val DEFAULT_MAX_RESULTS = 40
  }
}

suspend fun <T> safeApiCall(tag: String = BookService.TAG, call: suspend () -> T): T {
  return try {
    call()
  } catch (e: Exception) {
    Logger.e(tag, "API call failed", e)
    throw when (e) {
      is UnknownHostException -> AppError.Network.NoConnection(cause = e)
      is SocketTimeoutException ->
          AppError.Network.ServerError(message = "Request timed out", cause = e)
      is HttpException ->
          AppError.Network.ServerError(message = "Server returned error ${e.code()}", cause = e)
      else -> AppError.Network.ServerError(message = "Network request failed", cause = e)
    }
  }
}
