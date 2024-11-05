package com.example.bookapp.domain.model

data class Book(
    val title: String,
    val author: String,
    val description: String = "",
    val thumbnailUrl: String = "",
    val publishedDate: String = "",
    val pageCount: Int = 0,
    val categories: List<String> = emptyList()
)