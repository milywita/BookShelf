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
import com.example.bookapp.domain.error.AppError
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
  companion object {
    private const val TAG = "BookSync"
  }

  private val currentUserId: String?
    get() = auth.currentUser?.uid

  suspend fun syncWithFirestore() {
    Log.d(TAG, "Starting sync with Firestore")
    val userId = currentUserId ?: throw AppError.Sync.AuthenticationError()

    try {
      val firestoreBooks = firestoreRepository.getUserBookStream().first()
      Log.d(TAG, "Retrieved ${firestoreBooks.size} books from Firestore")

      firestoreBooks.forEach { firebaseBook ->
        try {
          bookDao.insertBook(firebaseBook.toBook().toBookEntity(userId))
          Log.d(TAG, "Synchronized book: ${firebaseBook.id}")
        } catch (e: Exception) {
          Log.e(TAG, "Failed to sync book: ${firebaseBook.id}", e)
          throw AppError.Sync.ConflictError(
            itemId = firebaseBook.id,
            cause = e
          )
        }
      }
      Log.i(TAG, "Sync completed successfully")
    } catch (e: Exception) {
      Log.e(TAG, "Sync failed", e)
      throw when (e) {
        is AppError -> e
        else -> AppError.Sync.NetworkError(cause = e)
      }
    }
  }

  suspend fun saveBook(book: Book) {
    Log.d(TAG, "Attempting to save book: ${book.id}")
    val userId = currentUserId ?: throw AppError.Sync.AuthenticationError()

    try {
      bookDao.insertBook(book.toBookEntity(userId))
      Log.d(TAG, "Book saved locally: ${book.id}")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to save book locally: ${book.id}", e)
      throw AppError.Sync.StorageError(cause = e)
    }

    try {
      firestoreRepository.saveBook(book)
      Log.i(TAG, "Book saved to Firestore: ${book.id}")
    } catch (e: Exception) {
      Log.w(TAG, "Book saved locally but failed to sync to Firestore: ${book.id}", e)
      throw AppError.Sync.NetworkError(cause = e)
    }
  }

  fun getSavedBooks(): Flow<List<Book>> = currentUserId?.let { userId ->
    bookDao.getUserBooks(userId)
      .map { entities -> entities.map { it.toBook() } }
      .catch { e ->
        Log.e(TAG, "Error retrieving saved books", e)
        emit(emptyList())
      }
  } ?: flowOf(emptyList())

  suspend fun deleteBook(bookId: String) {
    Log.d(TAG, "Attempting to delete book: $bookId")
    val userId = currentUserId ?: throw AppError.Sync.AuthenticationError()

    try {
      bookDao.deleteBook(bookId, userId)
      Log.d(TAG, "Book deleted from local database: $bookId")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to delete book from local database: $bookId", e)
      throw AppError.Sync.StorageError(cause = e)
    }
    try {
      firestoreRepository.deleteBook(bookId)
      Log.i(TAG, "Book deleted from Firestore: $bookId")
    } catch (e: Exception) {
      Log.w(TAG, "Failed to delete book from Firestore (will sync later): $bookId", e)
    }
  }
}