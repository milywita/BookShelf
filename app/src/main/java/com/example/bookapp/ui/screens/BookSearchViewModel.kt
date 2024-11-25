// BookSearchViewModel.kt
/**
 * ViewModel handling book search functionality and state management.
 * Coordinates between UI layer and repositories for search and save operations.
 */
package com.example.bookapp.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookapp.data.di.NetworkModule
import com.example.bookapp.data.local.BookDatabase
import com.example.bookapp.data.repository.BookRepository
import com.example.bookapp.data.repository.BookSyncRepository
import com.example.bookapp.data.repository.FirestoreRepository
import com.example.bookapp.domain.model.Book
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.toCollection
import kotlinx.coroutines.launch

class BookSearchViewModel(application: Application) : AndroidViewModel(application) {
  private val database = BookDatabase.getDatabase(getApplication())
  private val bookRepository = BookRepository(
    bookService = NetworkModule.bookService,
    bookDao = database.bookDao()
  )
  private val firestoreRepository = FirestoreRepository()
  private val bookSyncRepository = BookSyncRepository(
    bookDao = database.bookDao(),
    firestoreRepository = firestoreRepository
  )

  private val _state = MutableStateFlow(SearchState())
  val state: StateFlow<SearchState> = _state.asStateFlow()

  private var _savedBookIds = mutableSetOf<String>()

  // Books from both local storage and cloud
  val savedBooks = bookSyncRepository.getSavedBooks()
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  init {
    // Initial sync with Firestore when ViewModel is created
    syncWithFirestore()

    viewModelScope.launch {
      savedBooks.collect { books ->
        _savedBookIds = books.map { it.id }.toMutableSet()
      }
    }
  }

  fun isBookSaved(bookId: String): Boolean {
    return _savedBookIds.contains(bookId)
  }

  private fun syncWithFirestore() {
    viewModelScope.launch {
      try {
        bookSyncRepository.syncWithFirestore()
      } catch (e: Exception) {
        _state.value = _state.value.copy(
          message = "Failed to sync with cloud: ${e.message}"
        )
      }
    }
  }

  fun onSearchQueryChange(query: String) {
    _state.value = _state.value.copy(searchQuery = query)
  }

  fun onSearchClick() {
    val currentQuery = state.value.searchQuery
    if (currentQuery.isBlank()) return

    viewModelScope.launch {
      try {
        val books = bookRepository.searchBooks(currentQuery)
        _state.value = _state.value.copy(books = books)
      } catch (e: Exception) {
        _state.value = _state.value.copy(
          message = "Search failed: ${e.message}"
        )
      }
    }
  }

  fun onBookClick(book: Book) {
    _state.value = _state.value.copy(selectedBook = book)
  }

  fun onBackClick() {
    _state.value = _state.value.copy(selectedBook = null)
  }

  fun saveBook(book: Book) {
    viewModelScope.launch {
      try {
        bookSyncRepository.saveBook(book)
        _state.value = _state.value.copy(
          message = "Book saved successfully!"
        )
      } catch (e: Exception) {
        _state.value = _state.value.copy(
          message = "Error saving book: ${e.message}"
        )
      }
    }
  }

  fun updateBookProgress(bookId: String, progress: Int) {
    viewModelScope.launch {
      try {
        bookSyncRepository.updateBookProgress(bookId, progress)
        _state.value = _state.value.copy(
          message = "Reading progress updated"
        )
      } catch (e: Exception) {
        _state.value = _state.value.copy(
          message = "Failed to update progress: ${e.message}"
        )
      }
    }
  }

  fun toggleBookLike(bookId: String, isLiked: Boolean) {
    viewModelScope.launch {
      try {
        bookSyncRepository.toggleBookLike(bookId, isLiked)
        _state.value = _state.value.copy(
          message = if (isLiked) "Book added to favorites" else "Book removed from favorites"
        )
      } catch (e: Exception) {
        _state.value = _state.value.copy(
          message = "Failed to update like status: ${e.message}"
        )
      }
    }
  }

  fun updateNotes(bookId: String, notes: String) {
    viewModelScope.launch {
      try {
        bookSyncRepository.updateNotes(bookId, notes)
        _state.value = _state.value.copy(
          message = "Notes updated successfully"
        )
      } catch (e: Exception) {
        _state.value = _state.value.copy(
          message = "Failed to update notes: ${e.message}"
        )
      }
    }
  }

  fun clearMessage() {
    _state.value = _state.value.copy(message = null)
  }

  fun toggleSavedBooks() {
    _state.value = _state.value.copy(
      isShowingSavedBooks = !state.value.isShowingSavedBooks
    )
  }

  fun deleteBook(bookId: String){
    viewModelScope.launch {
      try {
        bookSyncRepository.deleteBook(bookId)
        _state.value = _state.value.copy(
          message = "Book deleted successfully"
        )
      } catch (e: Exception) {
        _state.value = _state.value.copy(
          message = "Error deleting book: ${e.message}"
        )
      }
    }
  }
}