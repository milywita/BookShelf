// BookRepository.kt
/**
 * Repository layer that handles data operations between the Google Books API and local Room
 * database, providing a clean API for the ViewModel layer.
 */
package com.example.bookapp.data.repository

import com.example.bookapp.data.api.BookService
import com.example.bookapp.data.api.safeApiCall
import com.example.bookapp.data.local.dao.BookDao
import com.example.bookapp.domain.error.AppError
import com.example.bookapp.domain.model.Book
import com.example.bookapp.util.Logger
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BookRepository(
    private val bookService: BookService,
    private val bookDao: BookDao,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
  companion object {
    private const val TAG = "BookRepository"
  }

  suspend fun searchBooks(
      query: String,
      maxResults: Int = BookService.DEFAULT_MAX_RESULTS
  ): Result<List<Book>> =
      withContext(ioDispatcher) {
        try {
          Logger.d(TAG, "Searching books with query: $query, maxResults: $maxResults")
          val response = safeApiCall { bookService.searchBooks(query, maxResults) }
          val books = response.toBooks()
          Logger.d(TAG, "Found ${books.size} books for query: $query")
          Result.success(books)
        } catch (e: Exception) {
          val error =
              when (e) {
                is AppError -> e
                else -> AppError.Book.SearchFailed(cause = e)
              }
          Logger.e(TAG, "Search failed for query: $query", error)
          Result.failure(error)
        }
      }
}
