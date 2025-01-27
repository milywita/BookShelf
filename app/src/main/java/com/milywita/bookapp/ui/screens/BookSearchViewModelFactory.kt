// BookSearchViewModelFactory.kt

package com.milywita.bookapp.ui.screens

import android.app.Application
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory

object BookSearchViewModelFactory {
  fun provide(application: Application) = viewModelFactory {
    initializer { BookSearchViewModel(application) }
  }
}
