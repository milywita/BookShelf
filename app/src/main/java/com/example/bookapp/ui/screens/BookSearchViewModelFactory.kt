// BookSearchViewModelFactory.kt
/**
 * Factory for creating BookSearchViewModel instances. Provides application context to ViewModels.
 */
package com.example.bookapp.ui.screens

import android.app.Application
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

object BookSearchViewModelFactory {
  fun provide(application: Application) = viewModelFactory {
    initializer { BookSearchViewModel(application) }
  }
}
