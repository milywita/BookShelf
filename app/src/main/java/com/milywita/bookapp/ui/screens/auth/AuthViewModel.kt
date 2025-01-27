// AuthViewModel.kt
/**
 * ViewModel that manages authentication state and operations. Handles form validation, Firebase
 * authentication, and error states.
 */
package com.milywita.bookapp.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.milywita.bookapp.BookApplication
import com.milywita.bookapp.data.local.BookDatabase
import com.milywita.bookapp.data.repository.AuthRepository
import com.milywita.bookapp.domain.error.AppError
import com.milywita.bookapp.util.Logger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/** Represents the authentication state of the application. */
data class AuthState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val registrationSuccessful: Boolean = false,
    val isEmailVerified: Boolean = false,
    val verificationEmailSent: Boolean = false,
    val passwordResetEmailSent: Boolean = false,
    val emailChangeSuccessful: Boolean = false
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
                    if (user.isEmailVerified) {
                      Logger.d(TAG, "Sign in successful for verified user: ${user.uid}")
                      updateState { copy(isLoggedIn = true, isEmailVerified = true) }
                    } else {
                      Logger.d(TAG, "Sign in attempt by unverified user: ${user.uid}")
                      // Sign out the user since they're not verified
                      authRepository.signOut()
                      updateState {
                        copy(
                            isLoggedIn = false,
                            isEmailVerified = false,
                            error = "Please verify your email before signing in")
                      }
                    }
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
                    // Send verification email immediately after successful registration
                    try {
                      user.sendEmailVerification().await()
                      Logger.d(
                          TAG,
                          "Registration successful and verification email sent for user: ${user.uid}")
                      updateState {
                        copy(
                            registrationSuccessful = true,
                            verificationEmailSent = true,
                            error = null)
                      }
                    } catch (e: Exception) {
                      Logger.e(TAG, "Failed to send verification email", e)
                      updateState {
                        copy(
                            registrationSuccessful = true,
                            error =
                                "Account created but failed to send verification email. Please try resending.")
                      }
                    }
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

  fun sendPasswordResetEmail(email: String) =
      viewModelScope.launch {
        if (!isValidEmail(email)) {
          handleError(AppError.Auth.InvalidEmail())
          return@launch
        }

        try {
          updateState { copy(isLoading = true, error = null) }

          authRepository
              .sendPasswordResetEmail(email)
              .fold(
                  onSuccess = {
                    updateState { copy(passwordResetEmailSent = true, error = null) }
                    Logger.d(TAG, "Password reset email sent")
                  },
                  onFailure = { error ->
                    Logger.e(TAG, "Failed to send password reset email", error)
                    handleError(error as? AppError ?: AppError.Unexpected(cause = error))
                  })
        } finally {
          updateState { copy(isLoading = false) }
        }
      }

  fun updateEmail(newEmail: String) =
      viewModelScope.launch {
        if (!isValidEmail(newEmail)) {
          handleError(AppError.Auth.InvalidEmail())
          return@launch
        }

        try {
          updateState { copy(isLoading = true, error = null) }

          authRepository
              .updateEmail(newEmail)
              .fold(
                  onSuccess = {
                    updateState { copy(emailChangeSuccessful = true, error = null) }
                    Logger.d(TAG, "Email update verification sent")
                  },
                  onFailure = { error ->
                    Logger.e(TAG, "Failed to update email", error)
                    handleError(error as? AppError ?: AppError.Unexpected(cause = error))
                  })
        } finally {
          updateState { copy(isLoading = false) }
        }
      }

  private fun isValidEmail(email: String): Boolean {
    return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
  }

  fun resendVerificationEmail() =
      viewModelScope.launch {
        try {
          updateState { copy(isLoading = true, error = null) }

          val currentUser = authRepository.getCurrentUser()
          if (currentUser != null) {
            currentUser.sendEmailVerification().await()
            updateState { copy(verificationEmailSent = true, error = null) }
            Logger.d(TAG, "Verification email resent to user: ${currentUser.uid}")
          } else {
            updateState { copy(error = "No user logged in") }
          }
        } catch (e: Exception) {
          Logger.e(TAG, "Failed to resend verification email", e)
          handleError(AppError.Auth.ValidationError(message = "Failed to send verification email"))
        } finally {
          updateState { copy(isLoading = false) }
        }
      }
}
