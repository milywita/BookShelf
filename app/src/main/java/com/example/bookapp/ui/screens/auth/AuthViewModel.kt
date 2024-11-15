// AuthViewModel.kt
package com.example.bookapp.ui.screens.auth

import android.annotation.SuppressLint
import com.example.bookapp.data.repository.AuthRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
  // Add initial state check

  init {
    viewModelScope.launch {
      _state.value = _state.value.copy(
        isLoggedIn = authRepository.getCurrentUser() != null
      )
    }
  }
  // Add function to check if form is valid
  private fun isValidForm(email: String, password: String): Boolean {
    return email.isNotEmpty() && password.isNotEmpty() &&
            android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
  }
  // Handles sign-in logic
  @SuppressLint("SuspiciousIndentation")
  fun signIn(email: String, password: String) {

    viewModelScope.launch {
      if (!isValidForm(email, password)) {
        _state.value = _state.value.copy(error = "Please enter valid email and password")
          return@launch
      }
      // Update loading state
      _state.value = _state.value.copy(isLoading = true, error = null)
      try {
        // Attempt authentication
        authRepository.signIn(email, password)
        // Update success state
        _state.value = _state.value.copy(isLoggedIn = true)
      } catch (e: Exception) {
        // Update error state
        _state.value = _state.value.copy(error = e.message)
      } finally {
        // Reset loading state
        _state.value = _state.value.copy(isLoading = false)
      }
    }
  }

  fun signOut() {
    viewModelScope.launch {
      authRepository.signOut()
      _state.value = _state.value.copy(isLoggedIn = false)
    }
  }
}

// Data class representing UI state
data class AuthState(
    val isLoading: Boolean = false, // Shows loading status
    val isLoggedIn: Boolean = false, // Tracks authentication state
    val error: String? = null // Holds error messages
)

/*
MutableStateFlow: Holds mutable state with initial AuthState
StateFlow: Read-only state for UI observation
viewModelScope: Coroutine scope tied to ViewModel lifecycle
State management using data class copy()
 */