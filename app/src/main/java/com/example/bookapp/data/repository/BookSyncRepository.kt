// BookSyncRepository.kt
/**
 * Repository that handles synchronization between local Room database and Firebase firestore.
 * Provides a single source of truth for book data while maintaining offline capabilities.
 */
package com.example.bookapp.data.repository

import android.util.Log
import com.example.bookapp.data.local.dao.BookDao
import com.example.bookapp.data.local.entity.toBook
import com.example.bookapp.data.local.entity.toBookEntity
import com.example.bookapp.domain.model.Book
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class BookSyncRepository(
    private val bookDao: BookDao,
    private val firestoreRepository: FirestoreRepository,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
  sealed class SyncError : Exception(){
    class NetworkError(override val cause: Throwable) : SyncError() {
      override val message = "Network connection failed: ${cause.message}"
    }
    class AuthenticationError : SyncError() {
      override val message = "User not authenticated. Please log in again."
    }
    class SyncConflictError(private val bookId: String) : SyncError() {
      override val message = "Sync conflict detected for book: $bookId"
    }
    class StorageError(override val cause: Throwable) : SyncError() {
      override val message = "Local storage error: ${cause.message}"
    }
  }

  private val currentUserId: String?
    get() = auth.currentUser?.uid

  // Core sync operations
  suspend fun syncWithFirestore() {
    val userId = currentUserId ?: throw SyncError.AuthenticationError()

    try {
      val firestoreBooks = firestoreRepository.getUserBookStream().first()
      Log.d(TAG, "Syncing ${firestoreBooks.size} books from firestore")

      firestoreBooks.forEach { firebaseBook ->
        try {
          bookDao.insertBook(firebaseBook.toBook().toBookEntity(userId))
        } catch (e: Exception) {
          throw SyncError.SyncConflictError(firebaseBook.id)
        }
      }
    } catch (e: Exception){
      when (e) {
        is SyncError -> throw e
        else -> throw SyncError.NetworkError(e)
      }
    }
  }

  suspend fun saveBook(book: Book){
    val userId = currentUserId ?: throw SyncError.AuthenticationError()
     try{
        // Save locally first
       bookDao.insertBook(book.toBookEntity(userId))
     } catch (e: Exception){
       throw SyncError.StorageError(e)
     }

    try {
        // Then save to firestore
      firestoreRepository.saveBook(book)
    } catch (e: Exception){
      Log.w(TAG, "Book saved locally but failed to sync to firestore", e)
      throw SyncError.NetworkError(e)
    }
  }

  // Book queries
  fun getSavedBooks(): Flow<List<Book>> = currentUserId?.let { userId ->
    bookDao.getUserBooks(userId)
      .map { entities -> entities.map { it.toBook() } }
      .catch { e ->
        Log.e(TAG, "Error retrieving saved books", e)
        emit(emptyList())
      }
  } ?: flowOf(emptyList())

  companion object {
    private const val TAG = "BookSync"
  }
}
