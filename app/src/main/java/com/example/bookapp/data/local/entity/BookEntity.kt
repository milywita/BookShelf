package com.example.bookapp.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.bookapp.domain.model.Book

// Entity - structure our data in database
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

// Conversion functions
fun Book.toBookEntity() =
    BookEntity(
        id = id,
        title = title,
        author = author,
        description = description,
        thumbnailUrl = thumbnailUrl,
        publishedDate = publishedDate,
        pageCount = pageCount,
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
