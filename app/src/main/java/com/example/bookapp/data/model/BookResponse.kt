// BookResponse.kt
/**
 * Data classes for parsing Google Books API responses.
 * Uses Moshi annotations for JSON deserialization with validation.
 */
package com.example.bookapp.data.model

import android.util.Log
import com.example.bookapp.domain.error.AppError
import com.example.bookapp.domain.model.Book
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BookResponse(
    @Json(name = "items") val items: List<BookItem>? = null
) {
    companion object {
        private const val TAG = "BookResponse"
    }

    fun toBooks(): List<Book> {
        return try {
            items?.mapNotNull { bookItem ->
                try {
                    bookItem.toBook()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to parse book item", e)
                    null
                }
            } ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse book response", e)
            throw AppError.Network.ServerError(
                message = "Failed to parse API response",
                cause = e
            )
        }
    }
}

@JsonClass(generateAdapter = true)
data class BookItem(
    @Json(name = "id") val id: String? = null,
    @Json(name = "volumeInfo") val volumeInfo: VolumeInfo? = null
) {
    fun toBook(): Book {
        return Book(
            id = requireNotNull(id) { "Book ID cannot be null" },
            title = volumeInfo?.title ?: "Unknown Title",
            author = volumeInfo?.authors?.firstOrNull() ?: "Unknown Author",
            description = volumeInfo?.description ?: "",
            thumbnailUrl = volumeInfo?.imageLinks?.thumbnail ?: "",
            publishedDate = volumeInfo?.publishedDate ?: "",
            pageCount = volumeInfo?.pageCount ?: 0,
            categories = volumeInfo?.categories ?: emptyList()
        )
    }
}

@JsonClass(generateAdapter = true)
data class VolumeInfo(
    @Json(name = "title") val title: String? = null,
    @Json(name = "authors") val authors: List<String>? = null,
    @Json(name = "description") val description: String? = null,
    @Json(name = "imageLinks") val imageLinks: ImageLinks? = null,
    @Json(name = "publishedDate") val publishedDate: String? = null,
    @Json(name = "pageCount") val pageCount: Int? = null,
    @Json(name = "categories") val categories: List<String>? = null
)

@JsonClass(generateAdapter = true)
data class ImageLinks(
    @Json(name = "thumbnail") val thumbnail: String? = null
)