// ui/screens/BookSearchViewModel.kt
package com.example.bookapp.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel  // Change this import
import androidx.lifecycle.viewModelScope
import com.example.bookapp.data.di.NetworkModule
import com.example.bookapp.data.local.BookDatabase
import com.example.bookapp.data.repository.BookRepository
import com.example.bookapp.domain.model.Book
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class BookSearchViewModel(application: Application) : AndroidViewModel(application) {  // Change this line
    private val database = BookDatabase.getDatabase(getApplication())
    private val repository = BookRepository(
        bookService = NetworkModule.bookService,
        bookDao = database.bookDao()
    )

    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    fun onSearchQueryChange(query: String) {
        _state.value = _state.value.copy(searchQuery = query)
    }

    fun onSearchClick() {
        val currentQuery = state.value.searchQuery
        if (currentQuery.isBlank()) return

        viewModelScope.launch {
            try {
                val books = repository.searchBooks(currentQuery)
                _state.value = _state.value.copy(books = books)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onBookClick(book: Book) {
        _state.value = _state.value.copy(selectedBook = book)
    }

    fun onBackClick() {
        _state.value = _state.value.copy(selectedBook = null)
    }

    // Add new function to save book
    fun clearMessage() {
        _state.value = _state.value.copy(message = null)
    }

    fun saveBook(book: Book) {
        viewModelScope.launch {
            try {
                repository.saveBook(book)
                _state.value = _state.value.copy(message = "Book saved successfully!")
            } catch (e: Exception) {
                _state.value = _state.value.copy(message = "Error saving book: ${e.message}")
                e.printStackTrace()
            }
        }
    }
    val savedBooks: StateFlow<List<Book>> = repository.getSavedBooks()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Add this function
    fun toggleSavedBooks() {
        _state.value = _state.value.copy(
            isShowingSavedBooks = !state.value.isShowingSavedBooks
        )
    }
}