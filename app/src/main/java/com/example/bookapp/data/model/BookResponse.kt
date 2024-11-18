// BookResponse.kt
/**
 * Data classes for parsing Google Books API responses.
 * Uses Moshi annotations for JSON deserialization.
 */
package com.example.bookapp.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BookResponse(@Json(name = "items") val items: List<BookItem>? = null)

@JsonClass(generateAdapter = true)
data class BookItem(
    @Json(name = "id") val id: String? = null,
    @Json(name = "volumeInfo") val volumeInfo: VolumeInfo? = null
)

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
