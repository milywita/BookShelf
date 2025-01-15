// Book.kt
/**
 * Domain model representing a book throughout the application.
 * Provides default values for optional fields.
 */
package com.example.bookapp.domain.model

data class Book(
    val id: String = "",
    val title: String,
    val author: String,
    val description: String = "",
    val thumbnailUrl: String = "",
    val publishedDate: String = "",
    val pageCount: Int = 0,
    val categories: List<String> = emptyList(),
    val group: BookGroup = BookGroup.NONE
)