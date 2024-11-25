// BookEntity.kt
/**
 * Room entity representing a saved book in the local database.
 * Includes mapping functions to convert between domain and database models.
 */
package com.example.bookapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.bookapp.domain.model.Book

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val description: String,
    val thumbnailUrl: String,
    val publishedDate: String,
    val pageCount: Int,
    val categories: String
)

// Extension functions for model conversion
fun Book.toBookEntity() =
    BookEntity(
        id = id,
        title = title,
        author = author,
        description = description,
        thumbnailUrl = thumbnailUrl,
        publishedDate = publishedDate,
        pageCount = pageCount,
        // Categories stored as comma-separated string for database efficiency
        categories = categories.joinToString(","))

fun BookEntity.toBook() =
    Book(
        id = id,
        title = title,
        author = author,
        description = description,
        thumbnailUrl = thumbnailUrl,
        publishedDate = publishedDate,
        pageCount = pageCount,
        categories = categories.split(",").filter { it.isNotEmpty() })
