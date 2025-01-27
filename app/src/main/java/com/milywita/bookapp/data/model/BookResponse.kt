// BookResponse.kt
package com.milywita.bookapp.data.model

import com.milywita.bookapp.domain.error.AppError
import com.milywita.bookapp.domain.model.Book
import com.milywita.bookapp.util.Logger
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BookResponse(@Json(name = "items") val items: List<BookItem>? = null) {
  companion object {
    private const val TAG = "BookResponse"
  }

  fun toBooks(): List<Book> {
    return try {
      items?.mapNotNull { bookItem ->
        try {
          bookItem.toBook()
        } catch (e: Exception) {
          Logger.e(TAG, "Failed to parse book item", e) // Replaced Log.e
          null
        }
      } ?: emptyList()
    } catch (e: Exception) {
      Logger.e(TAG, "Failed to parse book response", e) // Replaced Log.e
      throw AppError.Network.ServerError(message = "Failed to parse API response", cause = e)
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
        thumbnailUrl = volumeInfo?.imageLinks?.thumbnail?.replace("http://", "https://") ?: "",
        publishedDate = volumeInfo?.publishedDate ?: "",
        pageCount = volumeInfo?.pageCount ?: 0,
        categories = volumeInfo?.categories ?: emptyList())
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
data class ImageLinks(@Json(name = "thumbnail") val thumbnail: String? = null)
