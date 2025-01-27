package com.milywita.bookapp.util

import android.util.Log
import com.milywita.bookapp.BuildConfig

/**
 * Centralized logging utility for consistent logging across the application. Handles both debug and
 * production logging with proper error tracking.
 */
object Logger {
  private const val MAX_TAG_LENGTH = 23
  private const val MAX_LOG_LENGTH = 4000

  /** Log debug message - only in debug builds */
  fun d(tag: String, message: String) {
    if (com.milywita.bookapp.BuildConfig.DEBUG) log(Log.DEBUG, tag, message)
  }

  /** Log info message - only in debug builds */
  fun i(tag: String, message: String) {
    if (com.milywita.bookapp.BuildConfig.DEBUG) log(Log.INFO, tag, message)
  }

  /** Log verbose message - only in debug builds */
  fun v(tag: String, message: String) {
    if (com.milywita.bookapp.BuildConfig.DEBUG) log(Log.VERBOSE, tag, message)
  }

  /** Log warning - kept in all builds */
  fun w(tag: String, message: String, throwable: Throwable? = null) {
    val fullMessage = throwable?.let { "$message\n${Log.getStackTraceString(it)}" } ?: message
    log(Log.WARN, tag, fullMessage)
  }

  /** Log error - kept in all builds */
  fun e(tag: String, message: String, throwable: Throwable? = null) {
    val fullMessage = throwable?.let { "$message\n${Log.getStackTraceString(it)}" } ?: message
    log(Log.ERROR, tag, fullMessage)
  }

  /**
   * Internal logging function that handles log chunking for large messages and tag length
   * restrictions
   */
  private fun log(priority: Int, tag: String, message: String) {
    val safeTag = tag.take(MAX_TAG_LENGTH)

    // Split the message into chunks if it's too long
    var i = 0
    val length = message.length
    while (i < length) {
      var newline = message.indexOf('\n', i)
      newline = if (newline != -1) newline else length
      do {
        val end = minOf(newline, i + MAX_LOG_LENGTH)
        val part = message.substring(i, end)
        when (priority) {
          Log.ERROR -> Log.e(safeTag, part)
          Log.WARN -> Log.w(safeTag, part)
          Log.INFO -> Log.i(safeTag, part)
          Log.DEBUG -> Log.d(safeTag, part)
          Log.VERBOSE -> Log.v(safeTag, part)
        }
        i = end
      } while (i < newline)
      i++
    }
  }

  /** Log network errors with additional details */
  fun logNetworkError(tag: String, url: String, errorCode: Int? = null, throwable: Throwable? = null) {
    val errorMessage = buildString {
      append("Network request failed")
      if (url.isNotEmpty()) append(" for URL: $url")
      if (errorCode != null) append(" with code: $errorCode")
    }
    e(tag, errorMessage, throwable)
  }

  /** Log API responses, obfuscating sensitive data */
  fun logApiResponse(tag: String, url: String, responseCode: Int, isSuccess: Boolean) {
    if (!isSuccess || com.milywita.bookapp.BuildConfig.DEBUG) {
      val message = "API Response: $responseCode for URL: ${url.maskSensitiveData()}"
      if (isSuccess) d(tag, message) else e(tag, message)
    }
  }

  /** Extension function to obfuscate sensitive information in URLs */
  private fun String.maskSensitiveData(): String {
    return replace(Regex("(api_key|key|token)=[^&]*"), "$1=REDACTED")
  }
}
