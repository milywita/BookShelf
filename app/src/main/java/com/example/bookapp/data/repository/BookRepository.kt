// BookRepository.kt
/**
 * Repository layer that handles data operations between the Google Books API and local Room
 * database, providing a clean API for the ViewModel layer.
 */
package com.example.bookapp.data.repository

import com.example.bookapp.data.api.BookService
import com.example.bookapp.data.local.dao.BookDao
import com.example.bookapp.domain.model.Book
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Log


class BookRepository(
    private val bookService: BookService,
    private val bookDao: BookDao,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    sealed class BookError : Exception() {
        data class NetworkError(override val cause: Throwable) : BookError()
        data object AuthError : BookError() {
            private fun readResolve(): Any = AuthError
        }

        data class DatabaseError(override val cause: Throwable) : BookError()
    }

    private val currentUserId: String?
        get() = auth.currentUser?.uid

    suspend fun searchBooks(query: String): Result<List<Book>> = withContext(Dispatchers.IO) {
        try {
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
            Result.success(books)
        } catch (e: Exception) {
            Log.e(TAG, "Search failed", e)
            Result.failure(BookError.NetworkError(e))
        }
    }

    suspend fun deleteBook(bookId: String) = withContext(Dispatchers.IO) {
        try {
            val userId = currentUserId ?: throw BookError.AuthError
            bookDao.deleteBook(bookId, userId)
        } catch (e: Exception) {
            throw BookError.DatabaseError(e)
        }
    }

    companion object {
        private const val TAG = "BookRepository"
    }
}
