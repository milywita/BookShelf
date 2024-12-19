package com.example.bookapp.data.model.firebase

import android.util.Log
import com.example.bookapp.domain.error.AppError
import com.example.bookapp.domain.model.Book

data class FirebaseBook(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val description: String = "",
    val thumbnailUrl: String = "",
    val publishedDate: String = "",
    val pageCount: Int = 0,
    val categories: List<String> = emptyList(),
    // Firebase-specific fields
    val savedDate: Long = System.currentTimeMillis(),
    val isLiked: Boolean = false,
    val readingProgress: Int = 0,
    val notes: String = ""
) {
    companion object {
        private const val TAG = "FirebaseBook"

        fun fromBook(book: Book): FirebaseBook {
            try {
                require(book.id.isNotBlank()) { "Book ID cannot be blank" }
                require(book.title.isNotBlank()) { "Book title cannot be blank" }
                require(book.author.isNotBlank()) { "Book author cannot be blank" }

                Log.d(TAG, "Converting Book to FirebaseBook: ${book.id}")
                return FirebaseBook(
                    id = book.id,
                    title = book.title,
                    author = book.author,
                    description = book.description,
                    thumbnailUrl = book.thumbnailUrl,
                    publishedDate = book.publishedDate,
                    pageCount = book.pageCount,
                    categories = book.categories
                ).also {
                    Log.d(TAG, "Successfully converted Book to FirebaseBook: ${book.id}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to convert Book to FirebaseBook: ${book.id}", e)
                throw AppError.Firestore.WriteError(
                    message = "Failed to prepare book for Firestore: ${e.message}",
                    documentId = book.id,
                    cause = e
                )
            }
        }
    }

    fun toBook(): Book {
        try {
            require(id.isNotBlank()) { "Firebase book ID cannot be blank" }
            require(title.isNotBlank()) { "Firebase book title cannot be blank" }
            require(author.isNotBlank()) { "Firebase book author cannot be blank" }

            Log.d(TAG, "Converting FirebaseBook to Book: $id")
            return Book(
                id = id,
                title = title,
                author = author,
                description = description,
                thumbnailUrl = thumbnailUrl,
                publishedDate = publishedDate,
                pageCount = pageCount,
                categories = categories
            ).also {
                Log.d(TAG, "Successfully converted FirebaseBook to Book: $id")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to convert FirebaseBook to Book: $id", e)
            throw AppError.Firestore.ReadError(
                message = "Failed to convert Firestore book: ${e.message}",
                documentId = id,
                cause = e
            )
        }
    }
}