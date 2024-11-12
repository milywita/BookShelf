package com.example.bookapp.ui.screens

import android.app.DownloadManager.Query
import com.example.bookapp.domain.model.Book

data class SearchState(
    val searchQuery: String = "",
    val books: List<Book> = emptyList(),
    val selectedBook: Book? = null,
    val message: String? = null,
    val isShowingSavedBooks: Boolean = false

)
