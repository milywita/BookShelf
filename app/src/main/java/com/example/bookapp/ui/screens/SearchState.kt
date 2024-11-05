package com.example.bookapp.ui.screens

import android.app.DownloadManager.Query
import com.example.bookapp.domain.model.Book

data class SearchState(
    val searchQuery: String = "",
    val books: List<Book> = emptyList(),

    // property to track selected book
    val selectedBook: Book? = null
)
