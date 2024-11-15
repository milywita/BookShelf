// AuthViewModelFactory.kt
package com.example.bookapp.ui.screens.auth

import com.example.bookapp.data.repository.AuthRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

// Implements ViewModelProvider.Factory to create ViewModels with dependencies
class AuthViewModelFactory(

    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {

    // handles type checking and casting
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AuthViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }

}

