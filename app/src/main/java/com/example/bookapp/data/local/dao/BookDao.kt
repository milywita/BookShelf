// BookDao.kt
/**
 * Data Access Object for book-related database operations.
 * Provides methods for querying and modifying locally saved books.
 */
package com.example.bookapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.bookapp.data.local.entity.BookEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BookDao {
  @Query("SELECT * FROM books") fun getAllBooks(): Flow<List<BookEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertBook(book: BookEntity)

  @Query("DELETE FROM books WHERE id = :bookId")
  suspend fun deleteBook(bookId: String)
}
