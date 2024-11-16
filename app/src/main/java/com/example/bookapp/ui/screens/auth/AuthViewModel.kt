// AuthViewModel.kt
package com.example.bookapp.ui.screens.auth

import android.annotation.SuppressLint
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookapp.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// Manages UI state using MutableStateFlow/StateFlow
class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

  // Internal mutable state that can be modified
  private val _state = MutableStateFlow(AuthState())
  // Public immutable state that UI can observe
  val state: StateFlow<AuthState> = _state.asStateFlow()

  // Checks if user is already logged in when ViewModel is created
  // viewModelScope for coroutine management
  // Updates state based on current user
  init {
    viewModelScope.launch {
      _state.value = _state.value.copy(isLoggedIn = authRepository.getCurrentUser() != null)
    }
  }

  //   Checks if email and password are valid
  //   Uses Android's built-in email pattern matcher
  //   Returns true/false based on validation
  private fun isValidForm(email: String, password: String): Boolean {
    return email.isNotEmpty() &&
        password.isNotEmpty() &&
        android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
  }

  // Handles sign-in logic
  // Tells Android Lint (code analysis tool) to ignore specific warnings
  // "SuspiciousIndentation" warning is about code formatting
  //TODO: Remove @SuppressLint when possible
  @SuppressLint("SuspiciousIndentation")
  // 1. Form Validation
  fun signIn(email: String, password: String) {
    viewModelScope.launch {
      if (!isValidForm(email, password)) {
        _state.value = _state.value.copy(error = "Please enter a valid email and password")
        return@launch
      }

      // 2. Start Loading
      _state.value = _state.value.copy(isLoading = true, error = null)
      try {
        // 3. Attempt Sign In
        val result = authRepository.signIn(email, password)

        // 4. Handle Result
        result.fold(
            onSuccess = {
              // 4a. Success - Update logged in state
              _state.value = _state.value.copy(isLoggedIn = true)
            },
            onFailure = { e ->
              // 4b. Failure - Show error message
              _state.value = _state.value.copy(error = "Account not found or incorrect password")
            })
      } catch (e: Exception) {
        // 5. Handle Unexpected Errors
        _state.value = _state.value.copy(error = "Account not found or incorrect password")
      } finally {
        // 6. Always Stop Loading
        _state.value = _state.value.copy(isLoading = false)
      }
    }
  }

  // Handles register logic
  fun register(email: String, password: String) {
    viewModelScope.launch {
      // 1. Form Validation
      if (!isValidForm(email, password)) {
        _state.value =
            _state.value.copy(
                error = "Email must be valid and password must be at least 6 characters")
        return@launch
      }

      // 2. Start Loading
      _state.value = _state.value.copy(isLoading = true, error = null)

      try {
        // 4. Handle Result
        val result = authRepository.register(email, password)
        result.fold(
            onSuccess = {
              // 4a. Success - Update registration state
              _state.value =
                  _state.value.copy(registrationSuccessful = true, isLoading = false, error = null)
            },
            onFailure = { e ->
              // 4b. Failure - Show error message
              _state.value = _state.value.copy(error = e.message)
            })
      } catch (e: Exception) {
        // 5. Handle Unexpected Errors
        _state.value = _state.value.copy(error = e.message)
      } finally {
        // 6. Always Stop Loading
        _state.value = _state.value.copy(isLoading = false)
      }
    }
  }

  fun signOut() {
    viewModelScope.launch {
      // 1. Call repository's signOut
      authRepository.signOut()

      // 2. Update state to reflect logged out status
      _state.value = _state.value.copy(isLoggedIn = false)
    }
  }

  fun updateErrorState(errorMessage: String) {
    _state.value = _state.value.copy(error = errorMessage)
  }
}

// Data class representing UI state
data class AuthState(
    val isLoading: Boolean = false, // Shows loading status
    val isLoggedIn: Boolean = false, // Tracks authentication state
    val error: String? = null, // Holds error messages
    val registrationSuccessful: Boolean = false // Shows success message
)

/*
MutableStateFlow: Holds mutable state with initial AuthState
StateFlow: Read-only state for UI observation
viewModelScope: Coroutine scope tied to ViewModel lifecycle
State management using data class copy()
 */


/*
SignOut diagram:

User clicks Sign Out
       ↓
SignOut function called
       ↓
Repository signOut called
       ↓
Firebase signs out user
       ↓
Update UI state (isLoggedIn = false)
       ↓
UI reacts to state change




UpdateErrorState diagram:

Error condition detected
       ↓
updateErrorState called
       ↓
State updated with new error
       ↓
UI shows error message

 */