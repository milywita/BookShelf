// BookRepository.kt
/**
 * Repository layer that handles data operations between the Google Books API
 * and local Room database, providing a clean API for the ViewModel layer.
 */

package com.example.bookapp.data.repository

import com.example.bookapp.data.api.BookService
import com.example.bookapp.data.local.dao.BookDao
import com.example.bookapp.data.local.entity.toBook
import com.example.bookapp.data.local.entity.toBookEntity
import com.example.bookapp.domain.model.Book
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BookRepository(private val bookService: BookService, private val bookDao: BookDao) {

  suspend fun searchBooks(query: String): List<Book> {
    return try {
      val response = bookService.searchBooks(query)
      // Map API response to domain model, providing default values for nullable fields
      response.items?.map { bookItem ->
        val volumeInfo = bookItem.volumeInfo
        Book(
            id = bookItem.id ?: "",
            title = volumeInfo?.title ?: "Unknown Title",
            author = volumeInfo?.authors?.firstOrNull() ?: "Unknown Author",
            description = volumeInfo?.description ?: "No description available",
            thumbnailUrl = volumeInfo?.imageLinks?.thumbnail ?: "",
            publishedDate = volumeInfo?.publishedDate ?: "Unknown date",
            pageCount = volumeInfo?.pageCount ?: 0,
            categories = volumeInfo?.categories ?: emptyList())
      } ?: emptyList()
    } catch (e: Exception) {
      emptyList()
    }
  }

  fun getSavedBooks(): Flow<List<Book>> {
    return bookDao.getAllBooks().map { entities -> entities.map { it.toBook() } }
  }

  suspend fun saveBook(book: Book) {
    bookDao.insertBook(book.toBookEntity())
  }
}
