// AuthViewModel.kt
/**
 * ViewModel that manages authentication state and operations. Handles form validation, Firebase
 * authentication, and error states.
 */
package com.example.bookapp.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookapp.BookApplication
import com.example.bookapp.data.local.BookDatabase
import com.example.bookapp.data.repository.AuthRepository
import com.example.bookapp.domain.error.AppError
import com.example.bookapp.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Represents the authentication state of the application. */
data class AuthState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val registrationSuccessful: Boolean = false
)

/**
 * ViewModel responsible for managing authentication state and operations. Handles login,
 * registration, and session management using Firebase Authentication.
 */
class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {
  companion object {
    private const val TAG = "AuthViewModel"
    private const val MIN_PASSWORD_LENGTH = 6
  }

  private val _state = MutableStateFlow(AuthState())
  val state: StateFlow<AuthState> = _state.asStateFlow()

  private val database by lazy { BookDatabase.getDatabase(BookApplication.getInstance()) }

  init {
    checkLoginState()
  }

  private fun checkLoginState() =
      viewModelScope.launch {
        try {
          updateState { copy(isLoggedIn = authRepository.getCurrentUser() != null) }
        } catch (e: Exception) {
          Logger.e(TAG, "Error checking login state", e)
          handleError(AppError.Auth.SessionExpired(cause = e))
        }
      }

  fun signOut() =
      viewModelScope.launch {
        try {
          authRepository.getCurrentUser()?.uid?.let { userId ->
            database.bookDao().clearUserBooks(userId)
          }
          authRepository.signOut()
          updateState { copy(isLoggedIn = false) }
          Logger.d(TAG, "User signed out successfully")
        } catch (e: Exception) {
          Logger.e(TAG, "Error during sign out", e)
          handleError(AppError.Auth.SignOutError(cause = e))
        }
      }

  private fun isValidForm(email: String, password: String): Boolean {
    return when {
      email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
        handleError(AppError.Auth.InvalidEmail())
        false
      }
      password.isEmpty() || password.length < MIN_PASSWORD_LENGTH -> {
        handleError(AppError.Auth.WeakPassword())
        false
      }
      else -> true
    }
  }

  fun signIn(email: String, password: String) =
      viewModelScope.launch {
        if (!isValidForm(email, password)) return@launch

        try {
          updateState { copy(isLoading = true, error = null) }

          authRepository
              .signIn(email, password)
              .fold(
                  onSuccess = { user ->
                    Logger.d(TAG, "Sign in successful for user: ${user.uid}")
                    updateState { copy(isLoggedIn = true) }
                  },
                  onFailure = { error ->
                    Logger.e(TAG, "Sign in failed", error)
                    handleError(error as? AppError ?: AppError.Unexpected(cause = error))
                  })
        } finally {
          updateState { copy(isLoading = false) }
        }
      }

  fun register(email: String, password: String) =
      viewModelScope.launch {
        if (!isValidForm(email, password)) return@launch

        try {
          updateState { copy(isLoading = true, error = null) }

          authRepository
              .register(email, password)
              .fold(
                  onSuccess = { user ->
                    Logger.d(TAG, "Registration successful for user: ${user.uid}")
                    updateState { copy(registrationSuccessful = true, error = null) }
                  },
                  onFailure = { error ->
                    Logger.e(TAG, "Registration failed", error)
                    handleError(error as? AppError ?: AppError.Unexpected(cause = error))
                  })
        } finally {
          updateState { copy(isLoading = false) }
        }
      }

  /** Centralized error handling for authentication errors */
  private fun handleError(error: AppError) {
    val message =
        when (error) {
          is AppError.Auth.InvalidCredentials -> error.message
          is AppError.Auth.UserNotFound -> error.message
          is AppError.Auth.WeakPassword -> error.message
          is AppError.Auth.EmailAlreadyInUse -> error.message
          is AppError.Auth.InvalidEmail -> error.message
          is AppError.Auth.SignOutError -> error.message
          is AppError.Auth.ValidationError -> error.message
          is AppError.Auth.SessionExpired -> error.message
          is AppError.Network.NoConnection -> "Please check your internet connection and try again"
          is AppError.Unexpected -> "An unexpected error occurred. Please try again"
          else -> "Authentication error occurred"
        }
    updateState { copy(error = message) }
  }

  fun updateErrorState(errorMessage: String) {
    handleError(AppError.Auth.ValidationError(message = errorMessage))
  }

  fun clearError() {
    updateState { copy(error = null) }
  }

  private fun updateState(update: AuthState.() -> AuthState) {
    _state.update(update)
  }
}
