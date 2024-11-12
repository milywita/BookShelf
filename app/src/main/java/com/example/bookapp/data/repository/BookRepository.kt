package com.example.bookapp.data.repository

import com.example.bookapp.data.api.BookService
import com.example.bookapp.data.local.dao.BookDao
import com.example.bookapp.data.local.entity.toBook
import com.example.bookapp.data.local.entity.toBookEntity
import com.example.bookapp.domain.model.Book
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Connects database and network operations

class BookRepository(private val bookService: BookService, private val bookDao: BookDao) {
  // Network operation - Get books from API
  suspend fun searchBooks(query: String): List<Book> {
    return try {
      val response = bookService.searchBooks(query)
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

  // Database operations
  fun getSavedBooks(): Flow<List<Book>> {
    return bookDao.getAllBooks().map { entities -> entities.map { it.toBook() } }
  }

  suspend fun saveBook(book: Book) {
    bookDao.insertBook(book.toBookEntity())
  }
}
