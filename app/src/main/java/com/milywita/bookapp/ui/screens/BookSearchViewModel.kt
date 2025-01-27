// BookSearchViewModel.kt
package com.milywita.bookapp.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.milywita.bookapp.data.di.NetworkModule
import com.milywita.bookapp.data.local.BookDatabase
import com.milywita.bookapp.data.model.firebase.FirebaseBook
import com.milywita.bookapp.data.repository.BookRepository
import com.milywita.bookapp.data.repository.BookSyncRepository
import com.milywita.bookapp.data.repository.FirestoreRepository
import com.milywita.bookapp.domain.error.AppError
import com.milywita.bookapp.domain.model.Book
import com.milywita.bookapp.util.Logger
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onEmpty
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class BookSearchEvent {
  data class SearchError(val error: AppError) : BookSearchEvent()

  data object SyncSuccess : BookSearchEvent()

  data class SyncError(val error: AppError) : BookSearchEvent()
}

class BookSearchViewModel(application: Application) : AndroidViewModel(application) {
  companion object {
    private const val TAG = "BookSearchViewModel"
  }

  private val viewModelJob = SupervisorJob()
  private val ioScope = CoroutineScope(viewModelJob + Dispatchers.IO)

  private val database = BookDatabase.getDatabase(getApplication())
  private val auth = FirebaseAuth.getInstance()
  private val bookRepository =
      BookRepository(
          bookService = com.milywita.bookapp.data.di.NetworkModule.bookService, bookDao = database.bookDao(), auth = auth)
  private val firestoreRepository = FirestoreRepository()
  private val bookSyncRepository =
      BookSyncRepository(
          bookDao = database.bookDao(), firestoreRepository = firestoreRepository, auth = auth)

  private val _state = MutableStateFlow(SearchState())
  val state = _state.asStateFlow()

  private val _events = MutableSharedFlow<BookSearchEvent>()

  private var _savedBookIds = mutableSetOf<String>()
  private val _firestoreBooks = mutableSetOf<String>()

  private val _localBooksExist = MutableStateFlow(false)
  val localBooksExist = _localBooksExist.asStateFlow()

  val savedBooks =
      bookSyncRepository
          .getSavedBooks()
          .catch { e ->
            Logger.e(TAG, "Failed to collect saved books", e)
            emit(emptyList())
          }
          .stateIn(
              scope = viewModelScope,
              started = SharingStarted.WhileSubscribed(5000),
              initialValue = emptyList())

  init {
    Logger.d(TAG, "Initializing BookSearchViewModel")
    setupObservers()
  }

  private fun setupObservers() {
    Logger.d(TAG, "Setting up observers")
    viewModelScope.launch {
      observeAuthState()
      observeFirestoreBooks()
      observeSavedBooks()
    }
  }

  private fun observeAuthState() {
    auth.addAuthStateListener { firebaseAuth ->
      val user = firebaseAuth.currentUser
      Logger.d(TAG, "Auth state changed, user ID: ${user?.uid}")

      viewModelScope.launch {
        if (user != null) {
          try {
            Logger.d(TAG, "Attempting to sync with Firestore for user: ${user.uid}")
            syncWithFirestore()
          } catch (e: Exception) {
            val error =
                when (e) {
                  is AppError -> e
                  else -> AppError.Sync.NetworkError(cause = e)
                }
            Logger.e(TAG, "Failed to sync with Firestore for user: ${user.uid}", error)
          }
        } else {
          Logger.d(TAG, "User logged out, clearing local state")
          viewModelJob.cancelChildren()
          clearLocalState()
          _state.value = SearchState()
        }
      }
    }
  }

  private fun observeFirestoreBooks() =
      ioScope.launch {
        Logger.d(TAG, "Starting Firestore books observation")
        firestoreRepository
            .getUserBookStream()
            .catch { e ->
              Logger.e(TAG, "Error in Firestore book stream", e)
              emit(emptyList())
            }
            .collect { firebaseBooks ->
              Logger.d(TAG, "Received ${firebaseBooks.size} books from Firestore stream")
              updateFirestoreBooks(firebaseBooks)
            }
      }

  private fun observeSavedBooks() =
      ioScope.launch {
        Logger.d(TAG, "Starting saved books observation")
        savedBooks
            .onEach { books ->
              Logger.d(TAG, "Updated saved books collection, size: ${books.size}")
              updateSavedBooks(books)
            }
            .onEmpty {
              Logger.d(TAG, "No saved books found")
              updateSavedBooks(emptyList())
            }
            .onCompletion { error ->
              error?.let { e ->
                val appError =
                    when (e) {
                      is AppError -> e
                      else -> AppError.Database.ReadError(cause = e)
                    }
                Logger.e(TAG, "Failed to observe saved books", appError)
                emit(emptyList())
              }
            }
            .collect()
      }

  private fun updateFirestoreBooks(books: List<FirebaseBook>) {
    Logger.d(TAG, "Updating Firestore books cache, size: ${books.size}")
    _firestoreBooks.clear()
    _firestoreBooks.addAll(books.map { it.id })
    checkLocalBooks()
  }

  private fun updateSavedBooks(books: List<Book>) {
    Logger.d(TAG, "Updating saved books cache, size: ${books.size}")
    _savedBookIds = books.map { it.id }.toMutableSet()
    checkLocalBooks()
  }

  private fun clearLocalState() {
    Logger.d(TAG, "Clearing local state")
    _firestoreBooks.clear()
    _savedBookIds.clear()
    _localBooksExist.value = false
  }

  private fun checkLocalBooks() =
      viewModelScope.launch {
        Logger.d(TAG, "Checking for local-only books")
        try {
          val localOnlyBooks =
              savedBooks.first().filter { book ->
                val isLocalOnly = !_firestoreBooks.contains(book.id)
                Logger.d(TAG, "Book ${book.id} (${book.title}) local only status: $isLocalOnly")
                isLocalOnly
              }
          _localBooksExist.value = localOnlyBooks.isNotEmpty()
          Logger.d(TAG, "Local-only books check complete. Found: ${localOnlyBooks.size} books")
        } catch (e: Exception) {
          Logger.e(TAG, "Failed to check local books", e)
        }
      }

  fun isBookSaved(bookId: String): Boolean {
    val isSaved = _savedBookIds.contains(bookId)
    Logger.d(TAG, "Checking if book $bookId is saved: $isSaved")
    return isSaved
  }

  private fun syncWithFirestore() =
      viewModelScope.launch {
        Logger.d(TAG, "Starting Firestore sync")
        try {
          bookSyncRepository.syncWithFirestore()
          checkLocalBooks()
          Logger.i(TAG, "Firestore sync completed successfully")
          emitEvent(BookSearchEvent.SyncSuccess)
        } catch (e: Exception) {
          val error =
              when (e) {
                is AppError -> e
                else -> AppError.Sync.NetworkError(cause = e)
              }
          Logger.e(TAG, "Firestore sync failed", error)
          emitEvent(BookSearchEvent.SyncError(error))
          updateState { copy(message = "Failed to sync with cloud: ${error.message}") }
        }
      }

  fun migrateLocalBooks() =
      viewModelScope.launch {
        Logger.d(TAG, "Starting local books migration")
        try {
          val localOnlyBooks = savedBooks.first().filter { !_firestoreBooks.contains(it.id) }
          Logger.d(TAG, "Found ${localOnlyBooks.size} local-only books to migrate")

          if (localOnlyBooks.isEmpty()) {
            Logger.d(TAG, "No books to migrate")
            updateState { copy(message = "No local-only books to migrate") }
            return@launch
          }

          localOnlyBooks.forEach { book ->
            Logger.d(TAG, "Migrating book: ${book.title} (${book.id})")
            firestoreRepository.saveBook(book)
          }

          Logger.i(TAG, "Successfully migrated ${localOnlyBooks.size} books")
          syncWithFirestore()
          updateState { copy(message = "${localOnlyBooks.size} books migrated successfully!") }
          checkLocalBooks()
        } catch (e: Exception) {
          val error =
              when (e) {
                is AppError -> e
                else -> AppError.Sync.NetworkError(cause = e)
              }
          Logger.e(TAG, "Migration failed", error)
          updateState { copy(message = "Error migrating books: ${error.message}") }
        }
      }

  fun onSearchQueryChange(query: String) {
    updateState { copy(searchQuery = query) }
  }

  fun onSearchClick() =
      viewModelScope.launch {
        val query = state.value.searchQuery
        if (query.isBlank()) {
          Logger.d(TAG, "Search query is blank, skipping search")
          return@launch
        }

        updateState { copy(isLoading = true) }
        Logger.d(TAG, "Attempting to search books with query: $query")

        try {
          val result = bookRepository.searchBooks(query)
          result.fold(
              onSuccess = { books ->
                Logger.i(TAG, "Successfully found ${books.size} books for query: $query")

                // Check if any of these books are in our saved books and missing thumbnails
                val savedBooksMap = savedBooks.value.associateBy { it.id }
                books.forEach { searchResult ->
                  savedBooksMap[searchResult.id]?.let { savedBook ->
                    if (savedBook.thumbnailUrl.isBlank() &&
                        searchResult.thumbnailUrl.isNotBlank()) {
                      // Update the saved book with the thumbnail URL
                      Logger.d(TAG, "Recovering thumbnail for book: ${savedBook.id}")
                      viewModelScope.launch {
                        saveBook(savedBook.copy(thumbnailUrl = searchResult.thumbnailUrl))
                      }
                    }
                  }
                }

                updateState { copy(books = books, isLoading = false) }
              },
              onFailure = { error ->
                val appError = error as? AppError ?: AppError.Unexpected(cause = error)
                Logger.e(TAG, "Search failed for query: $query", appError)
                emitEvent(BookSearchEvent.SearchError(appError))
                val message =
                    when (error) {
                      is AppError.Network.NoConnection -> "No internet connection"
                      is AppError.Network.ServerError -> "Server error occurred"
                      is AppError.Book.SearchFailed -> "Search failed: ${error.message}"
                      else -> error.message ?: "An unexpected error occurred"
                    }
                updateState { copy(message = message, isLoading = false) }
              })
        } catch (e: Exception) {
          val error = AppError.Unexpected(cause = e)
          Logger.e(TAG, "Unexpected error during search for query: $query", e)
          updateState { copy(message = error.message, isLoading = false) }
        }
      }

  fun saveBook(book: Book) =
      viewModelScope.launch {
        Logger.d(TAG, "Attempting to save book: ${book.id} (${book.title})")
        try {
          val existingBook = savedBooks.value.find { it.id == book.id }
          val isGroupUpdate = existingBook != null && existingBook.group != book.group

          // Create updated book preserving existing data
          val bookToSave =
              if (existingBook != null) {
                existingBook.copy(
                    group = book.group,
                    thumbnailUrl =
                        if (book.thumbnailUrl.isNotBlank()) book.thumbnailUrl
                        else existingBook.thumbnailUrl)
              } else {
                book
              }
          if (state.value.selectedBook?.id == book.id) {
            updateState { copy(selectedBook = bookToSave) }
          }

          bookSyncRepository.saveBook(bookToSave)
          Logger.i(TAG, "Successfully saved book: ${book.id}")

          val message =
              if (isGroupUpdate) {
                "Reading status updated to: ${book.group.displayName}"
              } else {
                "Book saved successfully!"
              }
          updateState { copy(message = message) }
        } catch (e: Exception) {
          val error =
              when (e) {
                is AppError -> e
                else -> AppError.Unexpected(cause = e)
              }
          Logger.e(TAG, "Failed to save book: ${book.id}", error)
          updateState { copy(message = "Error saving book: ${error.message}") }

          if (state.value.selectedBook?.id == book.id) {
            savedBooks.value
                .find { it.id == book.id }
                ?.let { originalBook -> updateState { copy(selectedBook = originalBook) } }
          }
        }
      }

  fun deleteBook(bookId: String) =
      viewModelScope.launch {
        Logger.d(TAG, "Attempting to delete book: $bookId")
        try {

          updateState { copy(selectedBook = null) }

          bookSyncRepository.deleteBook(bookId)
          Logger.i(TAG, "Successfully deleted book: $bookId")
          updateState { copy(message = "Book deleted successfully") }
        } catch (e: Exception) {
          val error =
              when (e) {
                is AppError -> e
                else -> AppError.Unexpected(cause = e)
              }
          Logger.e(TAG, "Failed to delete book: $bookId", error)
          updateState { copy(message = "Error deleting book: ${error.message}") }
        }
      }

  fun onBookClick(book: Book) {
    Logger.d(TAG, "Book clicked: ${book.id}")
    updateState { copy(selectedBook = book) }
  }

  fun onBackClick() {
    Logger.d(TAG, "Back navigation requested")
    updateState { copy(selectedBook = null) }
  }

  fun clearMessage() {
    Logger.d(TAG, "Clearing message")
    updateState { copy(message = null) }
  }

  fun toggleSavedBooks() {
    val showingSaved = !state.value.isShowingSavedBooks
    Logger.d(TAG, "Toggling saved books view to: $showingSaved")
    updateState { copy(isShowingSavedBooks = showingSaved) }
  }

  private fun updateState(update: SearchState.() -> SearchState) {
    _state.value = update(_state.value)
  }

  private suspend fun emitEvent(event: BookSearchEvent) {
    Logger.d(TAG, "Emitting event: ${event::class.simpleName}")
    _events.emit(event)
  }

  override fun onCleared() {
    Logger.d(TAG, "ViewModel being cleared")
    super.onCleared()
    viewModelJob.cancel()
  }
}
