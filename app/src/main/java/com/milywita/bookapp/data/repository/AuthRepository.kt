// AuthRepository.kt

package com.milywita.bookapp.data.repository

import com.milywita.bookapp.domain.error.AppError
import com.milywita.bookapp.util.Logger
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class AuthRepository {
  companion object {
    private const val TAG = "AuthRepository"
  }

  private val auth = FirebaseAuth.getInstance()

  suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
    Logger.d(TAG, "Attempting sign in for user: ${email.maskEmail()}")
    return try {
      val result = auth.signInWithEmailAndPassword(email, password).await()
      result.user?.let { user ->
        Logger.i(TAG, "Sign in successful for user: ${user.uid}")
        Result.success(user)
      }
          ?: run {
            Logger.e(TAG, "Sign in failed: No user returned")
            Result.failure(AppError.Auth.UserNotFound())
          }
    } catch (e: Exception) {
      val error =
          when (e) {
            is FirebaseAuthInvalidUserException -> AppError.Auth.UserNotFound(cause = e)
            is FirebaseAuthInvalidCredentialsException ->
                AppError.Auth.InvalidCredentials(cause = e)
            is FirebaseNetworkException -> AppError.Network.NoConnection(cause = e)
            else -> AppError.Unexpected(message = "Authentication error: ${e.message}", cause = e)
          }
      Logger.e(TAG, "Authentication failed", e)
      Result.failure(error)
    }
  }

  suspend fun register(email: String, password: String): Result<FirebaseUser> {
    Logger.d(TAG, "Attempting registration for user: ${email.maskEmail()}")
    return try {
      val result = auth.createUserWithEmailAndPassword(email, password).await()
      result.user?.let { user ->
        Logger.i(TAG, "Registration successful for user: ${user.uid}")
        Result.success(user)
      }
          ?: run {
            Logger.e(TAG, "Registration failed: No user returned")
            Result.failure(AppError.Unexpected("Registration failed: Account creation error"))
          }
    } catch (e: Exception) {
      Logger.e(TAG, "Registration failed for user: ${email.maskEmail()}", e)
      val error =
          when (e) {
            is FirebaseAuthWeakPasswordException -> AppError.Auth.WeakPassword(cause = e)
            is FirebaseAuthInvalidCredentialsException -> AppError.Auth.InvalidEmail(cause = e)
            is FirebaseAuthUserCollisionException -> AppError.Auth.EmailAlreadyInUse(cause = e)
            is FirebaseNetworkException -> AppError.Network.NoConnection(cause = e)
            else -> AppError.Unexpected(cause = e)
          }
      Result.failure(error)
    }
  }

  fun signOut() {
    val userId = auth.currentUser?.uid
    Logger.d(TAG, "Signing out user: $userId")
    try {
      auth.signOut()
      Logger.i(TAG, "Sign out successful for user: $userId")
    } catch (e: Exception) {
      Logger.e(TAG, "Sign out failed for user: $userId", e)
      throw AppError.Auth.SignOutError(cause = e)
    }
  }

  fun getCurrentUser(): FirebaseUser? {
    return auth.currentUser?.also { user -> Logger.d(TAG, "Current user retrieved: ${user.uid}") }
        ?: run {
          Logger.d(TAG, "No current user found")
          null
        }
  }

  private fun String.maskEmail(): String {
    return this.split("@").let { parts ->
      if (parts.size != 2) return this
      "${parts[0].take(2)}***@${parts[1]}"
    }
  }

  // Add password reset functionality
  suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
    Logger.d(TAG, "Attempting to send password reset email to: ${email.maskEmail()}")
    return try {
      auth.sendPasswordResetEmail(email).await()
      Logger.i(TAG, "Password reset email sent successfully to: ${email.maskEmail()}")
      Result.success(Unit)
    } catch (e: Exception) {
      Logger.e(TAG, "Failed to send password reset email", e)
      val error =
          when (e) {
            is FirebaseAuthInvalidUserException -> AppError.Auth.UserNotFound(cause = e)
            is FirebaseAuthInvalidCredentialsException -> AppError.Auth.InvalidEmail(cause = e)
            is FirebaseNetworkException -> AppError.Network.NoConnection(cause = e)
            else -> AppError.Unexpected(cause = e)
          }
      Result.failure(error)
    }
  }

  // Add email change functionality
  suspend fun updateEmail(newEmail: String): Result<Unit> {
    Logger.d(TAG, "Attempting to update email to: ${newEmail.maskEmail()}")
    return try {
      auth.currentUser?.let { user ->
        user.verifyBeforeUpdateEmail(newEmail).await()
        Logger.i(TAG, "Email update verification sent to: ${newEmail.maskEmail()}")
        Result.success(Unit)
      }
          ?: run {
            Logger.e(TAG, "No user logged in")
            Result.failure(AppError.Auth.UserNotFound())
          }
    } catch (e: Exception) {
      Logger.e(TAG, "Failed to update email", e)
      val error =
          when (e) {
            is FirebaseAuthInvalidCredentialsException -> AppError.Auth.InvalidEmail(cause = e)
            is FirebaseNetworkException -> AppError.Network.NoConnection(cause = e)
            else -> AppError.Unexpected(cause = e)
          }
      Result.failure(error)
    }
  }

  // Add method to handle action code
  suspend fun handleActionCode(code: String): Result<Unit> {
    Logger.d(TAG, "Handling action code")
    return try {
      auth.applyActionCode(code).await()
      Logger.i(TAG, "Action code applied successfully")
      Result.success(Unit)
    } catch (e: Exception) {
      Logger.e(TAG, "Failed to handle action code", e)
      Result.failure(AppError.Unexpected(cause = e))
    }
  }
}
