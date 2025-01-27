// BookService.kt
package com.milywita.bookapp.data.api

import com.milywita.bookapp.data.model.BookResponse
import com.milywita.bookapp.domain.error.AppError
import com.milywita.bookapp.util.Logger
import retrofit2.HttpException
import retrofit2.http.GET
import retrofit2.http.Query
import java.net.SocketTimeoutException
import java.net.UnknownHostException

interface BookService {
  @GET("volumes")
  suspend fun searchBooks(
      @Query("q") query: String,
      @Query("maxResults") maxResults: Int = DEFAULT_MAX_RESULTS,
      @Query("fields") fields: String = "items(id,volumeInfo)",
      @Query("orderBy") orderBy: String = "relevance"
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

    when (e) {
      is HttpException -> {
        val errorBody = e.response()?.errorBody()?.string()
        Logger.e(tag, "HTTP ${e.code()} Error: $errorBody")
      }
    }

    throw when (e) {
      is UnknownHostException -> AppError.Network.NoConnection(cause = e)
      is SocketTimeoutException ->
          AppError.Network.ServerError(message = "Request timed out", cause = e)
      is HttpException ->
          AppError.Network.ServerError(
              message =
                  "Server returned error ${e.code()} - ${e.response()?.errorBody()?.string()}",
              cause = e)
      else -> AppError.Network.ServerError(message = "Network request failed", cause = e)
    }
  }
}
