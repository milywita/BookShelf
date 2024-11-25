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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
    val registrationSuccessful: Boolean = false
)

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {
    private val _state = MutableStateFlow(AuthState())
    val state: StateFlow<AuthState> = _state.asStateFlow()

    private val database by lazy {
        BookDatabase.getDatabase(BookApplication.instance)
    }

    init {
        checkLoginState()
    }

    private fun checkLoginState() = viewModelScope.launch {
        _state.value = _state.value.copy(
            isLoggedIn = authRepository.getCurrentUser() != null
        )
    }

    fun signOut() = viewModelScope.launch {
        authRepository.getCurrentUser()?.uid?.let { userId ->
            database.bookDao().clearUserBooks(userId)
        }
        authRepository.signOut()
        _state.value = _state.value.copy(isLoggedIn = false)
    }

    private fun isValidForm(email: String, password: String): Boolean =
        email.isNotEmpty() &&
                password.isNotEmpty() &&
                android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()

    fun signIn(email: String, password: String) = viewModelScope.launch {
        if (!isValidForm(email, password)) {
            _state.value = _state.value.copy(
                error = "Please enter a valid email and password"
            )
            return@launch
        }

        try {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val result = authRepository.signIn(email, password)
            result.fold(
                onSuccess = {
                    _state.value = _state.value.copy(isLoggedIn = true)
                },
                onFailure = {
                    _state.value = _state.value.copy(
                        error = "Account not found or incorrect password"
                    )
                }
            )
        } finally {
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    fun register(email: String, password: String) = viewModelScope.launch {
        if (!isValidForm(email, password)) {
            _state.value = _state.value.copy(
                error = "Email must be valid and password must be at least 6 characters"
            )
            return@launch
        }

        try {
            _state.value = _state.value.copy(isLoading = true, error = null)
            authRepository.register(email, password).fold(
                onSuccess = {
                    _state.value = _state.value.copy(
                        registrationSuccessful = true,
                        isLoading = false,
                        error = null
                    )
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(error = e.message)
                }
            )
        } finally {
            _state.value = _state.value.copy(isLoading = false)
        }
    }

    fun updateErrorState(errorMessage: String) {
        _state.value = _state.value.copy(error = errorMessage)
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}