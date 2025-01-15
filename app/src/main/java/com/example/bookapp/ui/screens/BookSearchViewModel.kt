// BookSearchViewModel.kt
package com.example.bookapp.ui.screens

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookapp.data.di.NetworkModule
import com.example.bookapp.data.local.BookDatabase
import com.example.bookapp.data.model.firebase.FirebaseBook
import com.example.bookapp.data.repository.*
import com.example.bookapp.domain.error.AppError
import com.example.bookapp.domain.model.Book
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

sealed class BookSearchEvent {
  data class SearchError(val error: AppError) : BookSearchEvent()
  object SyncSuccess : BookSearchEvent()
  data class SyncError(val error: AppError) : BookSearchEvent()
}

class BookSearchViewModel(application: Application) : AndroidViewModel(application) {
  companion object {
    private const val TAG = "BookSearchViewModel"
  }

  private val viewModelJob = SupervisorJob()
  private val ioScope = CoroutineScope(viewModelJob + Dispatchers.IO)

  data class DebugInfo(
    val isVisible: Boolean = false,
    val currentUserId: String = "",
    val localBookIds: List<String> = emptyList(),
    val firestoreBookIds: List<String> = emptyList(),
    val localOnlyBookIds: List<String> = emptyList()
  )

  private val database = BookDatabase.getDatabase(getApplication())
  private val auth = FirebaseAuth.getInstance()
  private val bookRepository = BookRepository(
    bookService = NetworkModule.bookService,
    bookDao = database.bookDao(),
    auth = auth
  )
  private val firestoreRepository = FirestoreRepository()
  private val bookSyncRepository = BookSyncRepository(
    bookDao = database.bookDao(),
    firestoreRepository = firestoreRepository,
    auth = auth
  )

  private val _state = MutableStateFlow(SearchState())
  val state = _state.asStateFlow()

  private val _events = MutableSharedFlow<BookSearchEvent>()

  private val _debugInfo = MutableStateFlow(DebugInfo())
  val debugInfo = _debugInfo.asStateFlow()

  private var _savedBookIds = mutableSetOf<String>()
  private val _firestoreBooks = mutableSetOf<String>()

  private val _localBooksExist = MutableStateFlow(false)
  val localBooksExist = _localBooksExist.asStateFlow()

  val savedBooks = bookSyncRepository.getSavedBooks()
    .catch { e ->
      Log.e(TAG, "Failed to collect saved books", e)
      emit(emptyList())
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  init {
    Log.d(TAG, "Initializing BookSearchViewModel")
    setupObservers()
  }

  private fun setupObservers() {
    Log.d(TAG, "Setting up observers")
    viewModelScope.launch {
      observeAuthState()
      observeFirestoreBooks()
      observeSavedBooks()
    }
  }

  private fun observeAuthState() {
    auth.addAuthStateListener { firebaseAuth ->
      val user = firebaseAuth.currentUser
      Log.d(TAG, "Auth state changed, user ID: ${user?.uid}")

      viewModelScope.launch {
        if (user != null) {
          try {
            Log.d(TAG, "Attempting to sync with Firestore for user: ${user.uid}")
            syncWithFirestore()
          } catch (e: Exception) {
            val error = when (e) {
              is AppError -> e
              else -> AppError.Sync.NetworkError(cause = e)
            }
            Log.e(TAG, "Failed to sync with Firestore for user: ${user.uid}", error)
          }
        } else {
          Log.d(TAG, "User logged out, clearing local state")
          viewModelJob.cancelChildren()
          clearLocalState()
          _state.value = SearchState()
          _debugInfo.value = DebugInfo()
        }
      }
    }
  }

  private fun observeFirestoreBooks() = ioScope.launch {
    Log.d(TAG, "Starting Firestore books observation")
    firestoreRepository.getUserBookStream()
      .catch { e ->
        Log.e(TAG, "Error in Firestore book stream", e)
        emit(emptyList())
      }
      .collect { firebaseBooks ->
        Log.d(TAG, "Received ${firebaseBooks.size} books from Firestore stream")
        updateFirestoreBooks(firebaseBooks)
      }
  }

  private fun observeSavedBooks() = ioScope.launch {
    Log.d(TAG, "Starting saved books observation")
    savedBooks
      .onEach { books ->
        Log.d(TAG, "Updated saved books collection, size: ${books.size}")
        updateSavedBooks(books)
      }
      .onEmpty {
        Log.d(TAG, "No saved books found")
        updateSavedBooks(emptyList())
      }
      .onCompletion { error ->
        error?.let { e ->
          val appError = when (e) {
            is AppError -> e
            else -> AppError.Database.ReadError(cause = e)
          }
          Log.e(TAG, "Failed to observe saved books", appError)
          emit(emptyList())
        }
      }
      .collect()
  }

  private fun updateFirestoreBooks(books: List<FirebaseBook>) {
    Log.d(TAG, "Updating Firestore books cache, size: ${books.size}")
    _firestoreBooks.clear()
    _firestoreBooks.addAll(books.map { it.id })
    checkLocalBooks()
  }

  private fun updateSavedBooks(books: List<Book>) {
    Log.d(TAG, "Updating saved books cache, size: ${books.size}")
    _savedBookIds = books.map { it.id }.toMutableSet()
    checkLocalBooks()
  }

  private fun clearLocalState() {
    Log.d(TAG, "Clearing local state")
    _firestoreBooks.clear()
    _savedBookIds.clear()
    _localBooksExist.value = false
  }

  fun toggleDebugInfo() {
    val newVisibility = !_debugInfo.value.isVisible
    Log.d(TAG, "Toggling debug info visibility to: $newVisibility")
    _debugInfo.value = _debugInfo.value.copy(isVisible = newVisibility)
    if (newVisibility) {
      updateDebugInfo()
    }
  }

  private fun checkLocalBooks() = viewModelScope.launch {
    Log.d(TAG, "Checking for local-only books")
    try {
      val localOnlyBooks = savedBooks.first().filter { book ->
        val isLocalOnly = !_firestoreBooks.contains(book.id)
        Log.d(TAG, "Book ${book.id} (${book.title}) local only status: $isLocalOnly")
        isLocalOnly
      }
      _localBooksExist.value = localOnlyBooks.isNotEmpty()
      Log.d(TAG, "Local-only books check complete. Found: ${localOnlyBooks.size} books")
      updateDebugInfo()
    } catch (e: Exception) {
      Log.e(TAG, "Failed to check local books", e)
    }
  }

  private fun updateDebugInfo() = viewModelScope.launch {
    Log.d(TAG, "Updating debug information")
    try {
      val currentUser = auth.currentUser?.uid ?: "No user"
      val localBooks = savedBooks.first().map { it.id }
      val localOnlyBooks = savedBooks.first()
        .filter { !_firestoreBooks.contains(it.id) }
        .map { it.id }

      _debugInfo.value = _debugInfo.value.copy(
        currentUserId = currentUser,
        localBookIds = localBooks,
        firestoreBookIds = _firestoreBooks.toList(),
        localOnlyBookIds = localOnlyBooks
      )
      Log.d(TAG, "Debug info updated successfully")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to update debug info", e)
    }
  }

  fun isBookSaved(bookId: String): Boolean {
    val isSaved = _savedBookIds.contains(bookId)
    Log.d(TAG, "Checking if book $bookId is saved: $isSaved")
    return isSaved
  }

  private fun syncWithFirestore() = viewModelScope.launch {
    Log.d(TAG, "Starting Firestore sync")
    try {
      bookSyncRepository.syncWithFirestore()
      checkLocalBooks()
      Log.i(TAG, "Firestore sync completed successfully")
      emitEvent(BookSearchEvent.SyncSuccess)
    } catch (e: Exception) {
      val error = when (e) {
        is AppError -> e
        else -> AppError.Sync.NetworkError(cause = e)
      }
      Log.e(TAG, "Firestore sync failed", error)
      emitEvent(BookSearchEvent.SyncError(error))
      updateState { copy(message = "Failed to sync with cloud: ${error.message}") }
    }
  }

  fun migrateLocalBooks() = viewModelScope.launch {
    Log.d(TAG, "Starting local books migration")
    try {
      val localOnlyBooks = savedBooks.first().filter { !_firestoreBooks.contains(it.id) }
      Log.d(TAG, "Found ${localOnlyBooks.size} local-only books to migrate")

      if (localOnlyBooks.isEmpty()) {
        Log.d(TAG, "No books to migrate")
        updateState { copy(message = "No local-only books to migrate") }
        return@launch
      }

      localOnlyBooks.forEach { book ->
        Log.d(TAG, "Migrating book: ${book.title} (${book.id})")
        firestoreRepository.saveBook(book)
      }

      Log.i(TAG, "Successfully migrated ${localOnlyBooks.size} books")
      syncWithFirestore()
      updateState { copy(message = "${localOnlyBooks.size} books migrated successfully!") }
      checkLocalBooks()
    } catch (e: Exception) {
      val error = when (e) {
        is AppError -> e
        else -> AppError.Sync.NetworkError(cause = e)
      }
      Log.e(TAG, "Migration failed", error)
      updateState { copy(message = "Error migrating books: ${error.message}") }
    }
  }

  fun onSearchQueryChange(query: String) {
    updateState { copy(searchQuery = query) }
  }

  fun onSearchClick() = viewModelScope.launch {
    val query = state.value.searchQuery
    if (query.isBlank()) {
      Log.d(TAG, "Search query is blank, skipping search")
      return@launch
    }

    updateState { copy(isLoading = true) }
    Log.d(TAG, "Attempting to search books with query: $query")

    try {
      val result = bookRepository.searchBooks(query)
      result.fold(
        onSuccess = { books ->
          Log.i(TAG, "Successfully found ${books.size} books for query: $query")
          updateState { copy(books = books, isLoading = false) }
        },
        onFailure = { error ->
          val appError = error as? AppError ?: AppError.Unexpected(cause = error)
          Log.e(TAG, "Search failed for query: $query", appError)
          emitEvent(BookSearchEvent.SearchError(appError))
          val message = when (error) {
            is AppError.Network.NoConnection -> "No internet connection"
            is AppError.Network.ServerError -> "Server error occurred"
            is AppError.Book.SearchFailed -> "Search failed: ${error.message}"
            else -> error.message ?: "An unexpected error occurred"
          }
          updateState { copy(message = message, isLoading = false) }
        }
      )
    } catch (e: Exception) {
      val error = AppError.Unexpected(cause = e)
      Log.e(TAG, "Unexpected error during search for query: $query", e)
      updateState { copy(message = error.message, isLoading = false) }
    }
  }

  fun saveBook(book: Book) = viewModelScope.launch {
    Log.d(TAG, "Attempting to save book: ${book.id} (${book.title})")
    try {
      val existingBook = savedBooks.value.find { it.id == book.id }
      val isGroupUpdate = existingBook != null && existingBook.group != book.group

      // Update UI state immediately for the selected book
      if (state.value.selectedBook?.id == book.id) {
        updateState { copy(selectedBook = book) }
      }

      bookSyncRepository.saveBook(book)
      Log.i(TAG, "Successfully saved book: ${book.id}")

      val message = if (isGroupUpdate) {
        "Reading status updated to: ${book.group.displayName}"
      } else {
        "Book saved successfully!"
      }
      updateState { copy(message = message) }
    } catch (e: Exception) {
      val error = when (e) {
        is AppError -> e
        else -> AppError.Unexpected(cause = e)
      }
      Log.e(TAG, "Failed to save book: ${book.id}", error)
      updateState { copy(message = "Error saving book: ${error.message}") }

      // Revert UI state if save failed
      if (state.value.selectedBook?.id == book.id) {
        savedBooks.value.find { it.id == book.id }?.let { originalBook ->
          updateState { copy(selectedBook = originalBook) }
        }
      }
    }
  }

  fun deleteBook(bookId: String) = viewModelScope.launch {
    Log.d(TAG, "Attempting to delete book: $bookId")
    try {
      // Update UI state immediately
      updateState { copy(selectedBook = null) }

      // Delete from both local database and Firestore
      bookSyncRepository.deleteBook(bookId)
      Log.i(TAG, "Successfully deleted book: $bookId")
      updateState { copy(message = "Book deleted successfully") }
    } catch (e: Exception) {
      val error = when (e) {
        is AppError -> e
        else -> AppError.Unexpected(cause = e)
      }
      Log.e(TAG, "Failed to delete book: $bookId", error)
      updateState { copy(message = "Error deleting book: ${error.message}") }
    }
  }

  fun forceCheckLocalBooks() = viewModelScope.launch {
    Log.d(TAG, "Force checking local books")
    try {
      checkLocalBooks()
      updateDebugInfo()
      Log.d(TAG, "Force check completed successfully")
    } catch (e: Exception) {
      Log.e(TAG, "Force check failed", e)
    }
  }

  fun onBookClick(book: Book) {
    Log.d(TAG, "Book clicked: ${book.id}")
    updateState { copy(selectedBook = book) }
  }

  fun onBackClick() {
    Log.d(TAG, "Back navigation requested")
    updateState { copy(selectedBook = null) }
  }

  fun clearMessage() {
    Log.d(TAG, "Clearing message")
    updateState { copy(message = null) }
  }

  fun toggleSavedBooks() {
    val showingSaved = !state.value.isShowingSavedBooks
    Log.d(TAG, "Toggling saved books view to: $showingSaved")
    updateState { copy(isShowingSavedBooks = showingSaved) }
  }

  private fun updateState(update: SearchState.() -> SearchState) {
    _state.value = update(_state.value)
  }

  private suspend fun emitEvent(event: BookSearchEvent) {
    Log.d(TAG, "Emitting event: ${event::class.simpleName}")
    _events.emit(event)
  }

  override fun onCleared() {
    Log.d(TAG, "ViewModel being cleared")
    super.onCleared()
    viewModelJob.cancel()
  }
}