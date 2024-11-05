package com.example.bookapp.data.repository

import com.example.bookapp.data.api.BookService
import com.example.bookapp.domain.model.Book

class BookRepository(
    private val bookService: BookService
) {
    suspend fun searchBooks(query: String): List<Book> {
        return try {
            val response = bookService.searchBooks(query)
            response.items?.map { bookItem ->
                val volumeInfo = bookItem.volumeInfo
                Book(
                    title = volumeInfo?.title ?: "Unknown Title",
                    author = volumeInfo?.authors?.firstOrNull() ?: "Unknown Author",
                    description = volumeInfo?.description ?: "No description available",
                    thumbnailUrl = volumeInfo?.imageLinks?.thumbnail ?: "",
                    publishedDate = volumeInfo?.publishedDate ?: "Unknown date",
                    pageCount = volumeInfo?.pageCount ?: 0,
                    categories = volumeInfo?.categories ?: emptyList()
                )
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}