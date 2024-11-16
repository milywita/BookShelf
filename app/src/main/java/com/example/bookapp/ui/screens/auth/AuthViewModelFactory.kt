// AuthViewModelFactory.kt
package com.example.bookapp.ui.screens.auth

import com.example.bookapp.data.repository.AuthRepository
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

// Responsible for creating instances of AuthViewModel with the proper dependencies
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
/*
- Organization: Everything is set up properly
- Reusability: Can create ViewModels anywhere in the app
- Testing: Easy to provide test versions of dependencies
- Clean Code: Proper way to create ViewModels with dependencies
 */
