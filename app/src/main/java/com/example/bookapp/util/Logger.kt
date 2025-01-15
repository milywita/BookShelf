package com.example.bookapp.util

import android.util.Log
import com.example.bookapp.BuildConfig

/**
 * Centralized logging utility for consistent logging across the application. Automatically handles
 * debug vs production logging.
 */
object Logger {
  /** Log debug message - only in debug builds */
  fun d(tag: String, message: String) {
    if (BuildConfig.DEBUG) {
      Log.d(tag, message)
    }
  }

  /** Log info message - only in debug builds */
  fun i(tag: String, message: String) {
    if (BuildConfig.DEBUG) {
      Log.i(tag, message)
    }
  }

  /** Log verbose message - only in debug builds */
  fun v(tag: String, message: String) {
    if (BuildConfig.DEBUG) {
      Log.v(tag, message)
    }
  }

  /** Log warning - kept in all builds */
  fun w(tag: String, message: String, throwable: Throwable? = null) {
    if (throwable != null) {
      Log.w(tag, message, throwable)
    } else {
      Log.w(tag, message)
    }
  }

  /** Log error - kept in all builds */
  fun e(tag: String, message: String, throwable: Throwable? = null) {
    if (throwable != null) {
      Log.e(tag, message, throwable)
    } else {
      Log.e(tag, message)
    }
  }
}
