// BookRepository.kt
/**
 * Repository layer that handles data operations between the Google Books API and local Room
 * database, providing a clean API for the ViewModel layer.
 */
package com.example.bookapp.data.repository

import android.util.Log
import com.example.bookapp.domain.error.AppError
import com.example.bookapp.data.api.BookService
import com.example.bookapp.data.local.dao.BookDao
import com.example.bookapp.domain.model.Book
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.UnknownHostException
import java.net.SocketTimeoutException

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
            val response = bookService.searchBooks(query)
            val books = response.items?.map { bookItem ->
                val volumeInfo = bookItem.volumeInfo
                Book(
                    id = bookItem.id ?: "",
                    title = volumeInfo?.title ?: "Unknown Title",
                    author = volumeInfo?.authors?.firstOrNull() ?: "Unknown Author",
                    description = volumeInfo?.description ?: "No description available",
                    thumbnailUrl = volumeInfo?.imageLinks?.thumbnail ?: "",
                    publishedDate = volumeInfo?.publishedDate ?: "Unknown date",
                    pageCount = volumeInfo?.pageCount ?: 0,
                    categories = volumeInfo?.categories ?: emptyList()
                )
            } ?: emptyList()

            Log.d(TAG, "Found ${books.size} books for query: $query")
            Result.success(books)
        } catch (e: Exception) {
            val error = when (e) {
                is UnknownHostException -> AppError.Network.NoConnection(cause = e)
                is SocketTimeoutException -> AppError.Network.ServerError(cause = e)
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