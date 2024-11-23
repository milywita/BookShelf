package com.example.bookapp.data.model.firebase

data class FirebaseBook(
    val id: String = "",
    val title: String = "",
    val author: String = "",
    val description: String = "",
    val thumbnailUrl: String = "",
    val publishedDate: String = "",
    val pageCount: Int = 0,
    val categories: List<String> = emptyList(),
    // Firebase-specific fields
    val savedDate: Long = System.currentTimeMillis(),
    val isLiked: Boolean = false,
    val readingProgress: Int = 0, // TODO: Future implementation
    val notes: String = ""
) {
  companion object {
    fun fromBook(book: com.example.bookapp.domain.model.Book): FirebaseBook {
      return FirebaseBook(
          id = book.id,
          title = book.title,
          author = book.author,
          description = book.description,
          thumbnailUrl = book.thumbnailUrl,
          publishedDate = book.publishedDate,
          pageCount = book.pageCount,
          categories = book.categories)
    }
  }

  fun toBook(): com.example.bookapp.domain.model.Book {
    return com.example.bookapp.domain.model.Book(
        id = id,
        title = title,
        author = author,
        description = description,
        thumbnailUrl = thumbnailUrl,
        publishedDate = publishedDate,
        pageCount = pageCount,
        categories = categories)
  }
}
