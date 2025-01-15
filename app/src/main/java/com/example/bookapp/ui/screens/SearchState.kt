// SearchState.kt
/**
 * Data class representing the UI state for book search functionality.
 * Handles search query, results, selected book, messages, and view toggles.
 */
package com.example.bookapp.ui.screens

import com.example.bookapp.domain.model.Book

data class SearchState(
    val searchQuery: String = "",
    val books: List<Book> = emptyList(),
    val selectedBook: Book? = null,
    val message: String? = null,
    val isShowingSavedBooks: Boolean = false,
    val isLoading: Boolean = false
)
