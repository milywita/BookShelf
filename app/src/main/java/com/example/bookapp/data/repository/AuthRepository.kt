package com.example.bookapp.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

// Get Firebase Auth instance
class AuthRepository {
  private val auth =
      FirebaseAuth.getInstance() // gets a singleton instance of Firebase Authentication

  // This instance is stored privately as `auth` and will be used for all authentication operations

  // Sign in with email/password
  suspend fun signIn(email: String, password: String): Result<FirebaseUser> {
    return try {
      val result = auth.signInWithEmailAndPassword(email, password).await()
      result.user?.let { Result.success(it) } ?: Result.failure(Exception("Authentication failed"))
    } catch (e: Exception) {
      Result.failure(Exception("Sign in failed: ${e.message}"))
    }
  }

  /*
  signIn:
  - Calls Firebase's signInWithEmailAndPassword
  - Uses await() to wait for the async operation to complete
  - Returns success with user if login works
  - Returns failure with error message if something goes wrong
    - Uses try/catch to handle potential exceptions
    - Wraps errors in a Result.failure for clean error handling
   */

  // Register with email/password
  suspend fun register(email: String, password: String): Result<FirebaseUser> {
    return try {
      val result = auth.createUserWithEmailAndPassword(email, password).await()
      result.user?.let { Result.success(it) } ?: Result.failure(Exception("Registration failed"))
    } catch (e: Exception) {
      Result.failure(Exception("Registration failed: ${e.message}"))
    }
  }

  /*
  register:
  - Uses createUserWithEmailAndPassword instead of signIn
  - Same error handling pattern as signIn
  Returns:
  - Success with new user if registration works
  - Failure with error message if registration fails
   */

  // Sign out current user
  suspend fun signOut() {
    auth.signOut()
  }

  /*
  signOut:
  - Simple function that calls Firebase's signOut
  - No return value needed
   */

  // Get current signed-in user (null if not signed in)
  fun getCurrentUser(): FirebaseUser? = auth.currentUser
  /*
    getCurrentUser:
    - Not suspended because it's a synchronous* operation
    - Returns the currently signed-in user or null if no user is signed in
    - Used to check authentication state
     */
}

/*
*   Synchronous Operations (Sync):
    Execute immediately and block (wait) until completed.
    Run in order, one after another

    1. Gets called
    2. Immediately returns the current user
    3. Program continues
    4. No waiting needed because the data is already in memory

*   Asynchronous Operations (Async):
    Start execution but don't block (wait).
    Continue running other code while waiting

    1. Gets called
    2. Contacts Firebase servers (takes time)
    3. Program can do other things while waiting
    4. Result comes back later
 */