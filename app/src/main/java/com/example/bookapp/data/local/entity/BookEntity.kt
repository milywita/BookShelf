// BookEntity.kt
/**
 * Room entity representing a saved book in the local database. Includes mapping functions to
 * convert between domain and database models with validation and error handling.
 */
package com.example.bookapp.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.example.bookapp.domain.error.AppError
import com.example.bookapp.domain.model.Book
import com.example.bookapp.domain.model.BookGroup
import com.example.bookapp.util.Logger

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
    val group: String = BookGroup.NONE.name,
    val savedTimestamp: Long = System.currentTimeMillis()
) {
  companion object {
    const val TAG = "BookEntity"
  }
}

fun Book.toBookEntity(userId: String): BookEntity {
  try {
    require(id.isNotBlank()) { "Book ID cannot be blank" }
    require(userId.isNotBlank()) { "User ID cannot be blank" }
    require(title.isNotBlank()) { "Book title cannot be blank" }
    require(author.isNotBlank()) { "Book author cannot be blank" }

    Logger.d(BookEntity.TAG, "Converting Book to BookEntity: $id for user: $userId")
    return BookEntity(
            id = id,
            userId = userId,
            title = title,
            author = author,
            description = description,
            thumbnailUrl = thumbnailUrl,
            publishedDate = publishedDate,
            pageCount = pageCount,
            categories = categories.joinToString(","),
            group = group.name)
        .also { Logger.d(BookEntity.TAG, "Successfully converted Book to BookEntity: $id") }
  } catch (e: Exception) {
    Logger.e(BookEntity.TAG, "Failed to convert Book to BookEntity: $id", e)
    throw AppError.Database.WriteError(
        message = "Failed to create database entity: ${e.message}", cause = e)
  }
}

fun BookEntity.toBook(): Book {
  try {
    Logger.d(BookEntity.TAG, "Converting BookEntity to Book: $id")
    return Book(
            id = id,
            title = title,
            author = author,
            description = description,
            thumbnailUrl = thumbnailUrl,
            publishedDate = publishedDate,
            pageCount = pageCount,
            categories = categories.split(",").filter { it.isNotEmpty() },
            group = BookGroup.fromString(group))
        .also { Logger.d(BookEntity.TAG, "Successfully converted BookEntity to Book: $id") }
  } catch (e: Exception) {
    Logger.e(BookEntity.TAG, "Failed to convert BookEntity to Book: $id", e)
    throw AppError.Database.ReadError(
        message = "Failed to read database entity: ${e.message}", cause = e)
  }
}
