// AuthViewModel.kt
/**
 * ViewModel that manages authentication state and operations. Handles form validation, Firebase
 * authentication, and error states.
 */
package com.example.bookapp.ui.screens.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookapp.BookApplication
import com.example.bookapp.data.local.BookDatabase
import com.example.bookapp.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val registrationSuccessful: Boolean = false
)

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {
    companion object {
        private const val TAG = "AuthViewModel"
        private const val MIN_PASSWORD_LENGTH = 6
    }

    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val database by lazy {
        // Using the proper getter method from BookApplication
        BookDatabase.getDatabase(BookApplication.getInstance())
    }

    init {
        checkLoginState()
    }

    private fun checkLoginState() = viewModelScope.launch {
        try {
            updateState { copy(isLoggedIn = authRepository.getCurrentUser() != null) }
        } catch (e: Exception) {
            Log.e(TAG, "Error checking login state", e)
            updateState { copy(error = "Failed to check login state") }
        }
    }

    fun signOut() = viewModelScope.launch {
        try {
            authRepository.getCurrentUser()?.uid?.let { userId ->
                database.bookDao().clearUserBooks(userId)
            }
            authRepository.signOut()
            updateState { copy(isLoggedIn = false) }
        } catch (e: Exception) {
            Log.e(TAG, "Error during sign out", e)
            updateState { copy(error = "Failed to sign out properly") }
        }
    }

    private fun isValidForm(email: String, password: String): Boolean {
        return email.isNotEmpty() &&
                password.isNotEmpty() &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() &&
                password.length >= MIN_PASSWORD_LENGTH
    }

    fun signIn(email: String, password: String) = viewModelScope.launch {
        if (!isValidForm(email, password)) {
            updateState {
                copy(error = "Please enter a valid email and password (min ${MIN_PASSWORD_LENGTH} characters)")
            }
            return@launch
        }

        try {
            updateState { copy(isLoading = true, error = null) }

            authRepository.signIn(email, password).fold(
                onSuccess = {
                    updateState { copy(isLoggedIn = true) }
                    Log.d(TAG, "Sign in successful")
                },
                onFailure = { e ->
                    Log.e(TAG, "Sign in failed", e)
                    updateState {
                        copy(error = "Account not found or incorrect password")
                    }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during sign in", e)
            updateState { copy(error = "An unexpected error occurred") }
        } finally {
            updateState { copy(isLoading = false) }
        }
    }

    fun register(email: String, password: String) = viewModelScope.launch {
        if (!isValidForm(email, password)) {
            updateState {
                copy(error = "Email must be valid and password must be at least $MIN_PASSWORD_LENGTH characters")
            }
            return@launch
        }

        try {
            updateState { copy(isLoading = true, error = null) }

            authRepository.register(email, password).fold(
                onSuccess = {
                    Log.d(TAG, "Registration successful")
                    updateState {
                        copy(registrationSuccessful = true, error = null)
                    }
                },
                onFailure = { e ->
                    Log.e(TAG, "Registration failed", e)
                    updateState { copy(error = e.message) }
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during registration", e)
            updateState { copy(error = "An unexpected error occurred") }
        } finally {
            updateState { copy(isLoading = false) }
        }
    }

    fun updateErrorState(errorMessage: String) {
        updateState { copy(error = errorMessage) }
    }

    fun clearError() {
        updateState { copy(error = null) }
    }

    private fun updateState(update: AuthState.() -> AuthState) {
        _state.update(update)
    }
}