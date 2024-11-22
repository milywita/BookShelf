// AuthRepository.kt
/**
 * Repository handling Firebase Authentication operations.
 * Provides interface for login, registration, and session management.
 */
package com.example.bookapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

class AuthRepository {
  private val auth = FirebaseAuth.getInstance()

  suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
    return try {
      val result = auth.signInWithEmailAndPassword(email, password).await()
      result.user?.let { Result.success(it) } ?: Result.failure(Exception("Authentication failed"))
    } catch (e: Exception) {
      Result.failure(Exception("Sign in failed: ${e.message}"))
    }
  }

  suspend fun register(email: String, password: String): Result<FirebaseUser> {
    return try {
      val result = auth.createUserWithEmailAndPassword(email, password).await()
      result.user?.let { Result.success(it) } ?: Result.failure(Exception("Registration failed"))
    } catch (e: Exception) {
      Result.failure(Exception("Registration failed: ${e.message}"))
    }
  }

  fun signOut() {
    auth.signOut()
  }

  fun getCurrentUser(): FirebaseUser? = auth.currentUser
}