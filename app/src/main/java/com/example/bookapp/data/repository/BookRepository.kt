// BookRepository.kt
/**
 * Repository layer that handles data operations between the Google Books API and local Room
 * database, providing a clean API for the ViewModel layer.
 */
package com.example.bookapp.data.repository

import android.util.Log
import com.example.bookapp.domain.error.AppError
import com.example.bookapp.data.api.BookService
import com.example.bookapp.data.api.safeApiCall
import com.example.bookapp.data.local.dao.BookDao
import com.example.bookapp.domain.model.Book
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BookRepository(
    private val bookService: BookService,
    private val bookDao: BookDao,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    companion object {
        private const val TAG = "BookRepository"
    }

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    suspend fun searchBooks(query: String): Result<List<Book>> = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Searching books with query: $query")
            val response = safeApiCall { bookService.searchBooks(query) }
            val books = response.toBooks()
            Log.d(TAG, "Found ${books.size} books for query: $query")
            Result.success(books)
        } catch (e: Exception) {
            val error = when (e) {
                is AppError -> e
                else -> AppError.Book.SearchFailed(cause = e)
            }
            Log.e(TAG, "Search failed for query: $query", error)
            Result.failure(error)
        }
    }

    suspend fun deleteBook(bookId: String) = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Attempting to delete book: $bookId")
            val userId = currentUserId ?: throw AppError.Auth.UserNotFound()
            bookDao.deleteBook(bookId, userId)
            Log.d(TAG, "Successfully deleted book: $bookId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete book: $bookId", e)
            throw when (e) {
                is AppError.Auth.UserNotFound -> e
                else -> AppError.Book.DeleteFailed(bookId = bookId, cause = e)
            }
        }
    }
}