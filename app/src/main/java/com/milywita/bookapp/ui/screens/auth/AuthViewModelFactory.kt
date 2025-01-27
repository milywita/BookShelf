// AuthViewModelFactory.kt
/**
 * Factory for creating AuthViewModel instances with dependency injection. Ensures ViewModels are
 * created with proper repository dependencies.
 */
package com.milywita.bookapp.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.milywita.bookapp.data.repository.AuthRepository

class AuthViewModelFactory(private val authRepository: AuthRepository) : ViewModelProvider.Factory {

  override fun <T : ViewModel> create(modelClass: Class<T>): T {
    if (modelClass.isAssignableFrom(AuthViewModel::class.java)) {
      @Suppress("UNCHECKED_CAST")
      return AuthViewModel(authRepository) as T
    }
    throw IllegalArgumentException("Unknown ViewModel class")
  }
}
