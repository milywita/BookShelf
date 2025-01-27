// BookRepository.kt

package com.milywita.bookapp.data.repository

import com.milywita.bookapp.data.api.BookService
import com.milywita.bookapp.data.api.safeApiCall
import com.milywita.bookapp.data.local.dao.BookDao
import com.milywita.bookapp.domain.error.AppError
import com.milywita.bookapp.domain.model.Book
import com.milywita.bookapp.util.Logger
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
