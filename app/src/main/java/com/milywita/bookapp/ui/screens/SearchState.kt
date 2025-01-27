// SearchState.kt

package com.milywita.bookapp.ui.screens

import com.milywita.bookapp.domain.model.Book

data class SearchState(
    val searchQuery: String = "",
    val books: List<Book> = emptyList(),
    val selectedBook: Book? = null,
    val message: String? = null,
    val isShowingSavedBooks: Boolean = false,
    val isLoading: Boolean = false
)
