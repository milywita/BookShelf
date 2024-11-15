package com.example.bookapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

// Get Firebase Auth instance
class AuthRepository {
  private val auth = FirebaseAuth.getInstance()

  // Sign in with email/password
  suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
    return try {
      // await() suspends until auth completes
      val result = auth.signInWithEmailAndPassword(email, password).await()
      Result.success(result.user!!) // Returns success with user
    } catch (e: Exception) {
      Result.failure(e) // Returns failure with exception
    }
  }

  // Sign out current user
  suspend fun signOut() {
    auth.signOut()
  }

  // Get current signed-in user (null if not signed in)
  fun getCurrentUser(): FirebaseUser? = auth.currentUser
}
