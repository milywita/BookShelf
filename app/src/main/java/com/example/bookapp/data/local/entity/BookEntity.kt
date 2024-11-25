// BookEntity.kt
/**
 * Room entity representing a saved book in the local database. Includes mapping functions to
 * convert between domain and database models.
 */
package com.example.bookapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.bookapp.domain.model.Book

@Entity(
    tableName = "books",
    indices = [Index(value = ["userId"]), Index(value = ["id", "userId"], unique = true)])
data class BookEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val author: String,
    val description: String,
    val thumbnailUrl: String,
    val publishedDate: String,
    val pageCount: Int,
    val categories: String,
    val savedTimestamp: Long = System.currentTimeMillis()
)

fun Book.toBookEntity(userId: String) = BookEntity(
    id = id,
    userId = userId,
    title = title,
    author = author,
    description = description,
    thumbnailUrl = thumbnailUrl,
    publishedDate = publishedDate,
    pageCount = pageCount,
    categories = categories.joinToString(",")
)

fun BookEntity.toBook() = Book(
    id = id,
    title = title,
    author = author,
    description = description,
    thumbnailUrl = thumbnailUrl,
    publishedDate = publishedDate,
    pageCount = pageCount,
    categories = categories.split(",").filter { it.isNotEmpty() }
)