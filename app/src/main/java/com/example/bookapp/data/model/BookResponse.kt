package com.example.bookapp.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class BookResponse(
    @Json(name = "items") val items: List<BookItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class BookItem(
    @Json(name = "id") val id: String,
    @Json(name = "volumeInfo") val volumeInfo: VolumeInfo
)

@JsonClass(generateAdapter = true)
data class VolumeInfo(
    @Json(name = "title") val title: String = "",
    @Json(name = "authors") val authors: List<String> = emptyList(),
    @Json(name = "description") val description: String = "",
    @Json(name = "imageLinks") val imageLinks: ImageLinks? = null
)

@JsonClass(generateAdapter = true)
data class ImageLinks(
    @Json(name = "thumbnail") val thumbnail: String = ""
)