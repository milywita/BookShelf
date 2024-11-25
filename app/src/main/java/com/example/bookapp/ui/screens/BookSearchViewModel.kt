package com.example.bookapp.ui.screens

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookapp.data.di.NetworkModule
import com.example.bookapp.data.local.BookDatabase
import com.example.bookapp.data.model.firebase.FirebaseBook
import com.example.bookapp.data.repository.*
import com.example.bookapp.domain.model.Book
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

sealed class BookSearchEvent {
  data class SearchError(val error: Throwable) : BookSearchEvent()
  object SyncSuccess : BookSearchEvent()
  data class SyncError(val error: Throwable) : BookSearchEvent()
}

class BookSearchViewModel(application: Application) : AndroidViewModel(application) {
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
      logError("Error collecting saved books", e)
      emit(emptyList())
    }
    .stateIn(
      scope = viewModelScope,
      started = SharingStarted.WhileSubscribed(5000),
      initialValue = emptyList()
    )

  init {
    setupObservers()
  }

  private fun setupObservers() {
    viewModelScope.launch {
      observeAuthState()
      observeFirestoreBooks()
      observeSavedBooks()
    }
  }

  private fun observeAuthState() {
    auth.addAuthStateListener { firebaseAuth ->
      val user = firebaseAuth.currentUser
      logDebug("Auth state changed, user: ${user?.uid}")

      viewModelScope.launch {
        if (user != null) {
          try {
            syncWithFirestore()
          } catch (e: Exception) {
            logError("Error syncing with Firestore", e)
          }
        } else {
          // Cancel ongoing operations first
          viewModelJob.cancelChildren()
          clearLocalState()

          // Reset state
          _state.value = SearchState()
          _debugInfo.value = DebugInfo()
        }
      }
    }
  }

  private fun observeFirestoreBooks() = ioScope.launch {
    firestoreRepository.getUserBookStream()
      .catch { e ->
        logError("Error in Firestore stream", e)
        emit(emptyList())
      }
      .collect { firebaseBooks ->
        logDebug("Received ${firebaseBooks.size} books from Firestore")
        updateFirestoreBooks(firebaseBooks)
      }
  }

  private fun observeSavedBooks() = ioScope.launch {
    savedBooks
      .catch { e ->
        Log.e(TAG, "Error collecting saved books", e)
        emitAll(flowOf(emptyList()))
      }
      .collect { books ->
        logDebug("Received ${books.size} saved books")
        updateSavedBooks(books)
      }
  }

  private fun updateFirestoreBooks(books: List<FirebaseBook>) {
    _firestoreBooks.clear()
    _firestoreBooks.addAll(books.map { it.id })
    checkLocalBooks()
  }

  private fun updateSavedBooks(books: List<Book>) {
    _savedBookIds = books.map { it.id }.toMutableSet()
    checkLocalBooks()
  }

  private fun clearLocalState() {
    _firestoreBooks.clear()
    _savedBookIds.clear()
    _localBooksExist.value = false
  }

  fun toggleDebugInfo() {
    _debugInfo.value = _debugInfo.value.copy(isVisible = !_debugInfo.value.isVisible)
    if (_debugInfo.value.isVisible) {
      updateDebugInfo()
    }
  }

  private fun checkLocalBooks() = viewModelScope.launch {
    try {
      val localOnlyBooks = savedBooks.first().filter { book ->
        val isLocalOnly = !_firestoreBooks.contains(book.id)
        logDebug("Book ${book.title} (${book.id}) is local only: $isLocalOnly")
        isLocalOnly
      }
      _localBooksExist.value = localOnlyBooks.isNotEmpty()
      updateDebugInfo()
    } catch (e: Exception) {
      logError("Error checking local books", e)
    }
  }

  private fun updateDebugInfo() = viewModelScope.launch {
    try {
      _debugInfo.value = _debugInfo.value.copy(
        currentUserId = auth.currentUser?.uid ?: "No user",
        localBookIds = savedBooks.first().map { it.id },
        firestoreBookIds = _firestoreBooks.toList(),
        localOnlyBookIds = savedBooks.first()
          .filter { !_firestoreBooks.contains(it.id) }
          .map { it.id }
      )
    } catch (e: Exception) {
      logError("Error updating debug info", e)
    }
  }

  fun isBookSaved(bookId: String) = _savedBookIds.contains(bookId)

  private fun syncWithFirestore() = viewModelScope.launch {
    try {
      logDebug("Starting Firestore sync")
      bookSyncRepository.syncWithFirestore()
      checkLocalBooks()
      emitEvent(BookSearchEvent.SyncSuccess)
    } catch (e: Exception) {
      logError("Sync failed", e)
      emitEvent(BookSearchEvent.SyncError(e))
      updateState { copy(message = "Failed to sync with cloud: ${e.message}") }
    }
  }

  fun migrateLocalBooks() = viewModelScope.launch {
    try {
      val localOnlyBooks = savedBooks.first().filter { !_firestoreBooks.contains(it.id) }
      logDebug("Found ${localOnlyBooks.size} local-only books to migrate")

      if (localOnlyBooks.isEmpty()) {
        updateState { copy(message = "No local-only books to migrate") }
        return@launch
      }

      localOnlyBooks.forEach { book ->
        logDebug("Migrating book: ${book.title}")
        firestoreRepository.saveBook(book)
      }

      syncWithFirestore()
      updateState { copy(message = "${localOnlyBooks.size} books migrated successfully!") }
      checkLocalBooks()
    } catch (e: Exception) {
      logError("Migration failed", e)
      updateState { copy(message = "Error migrating books: ${e.message}") }
    }
  }

  fun onSearchQueryChange(query: String) {
    updateState { copy(searchQuery = query) }
  }

  fun onSearchClick() = viewModelScope.launch {
    val query = state.value.searchQuery
    if (query.isBlank()) return@launch

    try {
      val result = bookRepository.searchBooks(query)
      result.fold(
        onSuccess = { books -> updateState { copy(books = books) } },
        onFailure = { e ->
          emitEvent(BookSearchEvent.SearchError(e))
          updateState { copy(message = "Search failed: ${e.message}") }
        }
      )
    } catch (e: Exception) {
      updateState { copy(message = "Search failed: ${e.message}") }
    }
  }

  fun onBookClick(book: Book) = updateState { copy(selectedBook = book) }
  fun onBackClick() = updateState { copy(selectedBook = null) }
  fun clearMessage() = updateState { copy(message = null) }
  fun toggleSavedBooks() = updateState { copy(isShowingSavedBooks = !isShowingSavedBooks) }

  private fun updateState(update: SearchState.() -> SearchState) {
    _state.value = update(_state.value)
  }

  private suspend fun emitEvent(event: BookSearchEvent) {
    _events.emit(event)
  }

  private fun logDebug(message: String) = Log.d(TAG, message)
  private fun logError(message: String, error: Throwable? = null) = Log.e(TAG, message, error)

  override fun onCleared() {
    super.onCleared()
    viewModelJob.cancel()
  }

  companion object {
    private const val TAG = "BookSearchViewModel"
  }

  fun saveBook(book: Book) = viewModelScope.launch {
    try {
      bookSyncRepository.saveBook(book)
      updateState { copy(message = "Book saved successfully!") }
    } catch (e: Exception) {
      logError("Save failed", e)
      updateState { copy(message = "Error saving book: ${e.message}") }
    }
  }

  fun deleteBook(bookId: String) = viewModelScope.launch {
    try {
      bookRepository.deleteBook(bookId)
      firestoreRepository.deleteBook(bookId)
      updateState { copy(message = "Book deleted successfully") }
    } catch (e: Exception) {
      logError("Delete failed", e)
      updateState { copy(message = "Error deleting book: ${e.message}") }
    }
  }

  fun forceCheckLocalBooks() = viewModelScope.launch {
    try {
      checkLocalBooks()
      updateDebugInfo()
    } catch (e: Exception) {
      logError("Force check failed", e)
    }
  }
}