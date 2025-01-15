// BookDao.kt
/**
 * Data Access Object for book-related database operations. Note: Error handling is provided by
 * BookDaoWrapper.
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
  @Query("SELECT * FROM books WHERE userId = :userId ORDER BY title ASC")
  fun getUserBooks(userId: String): Flow<List<BookEntity>>

  @Query("SELECT * FROM books ORDER BY title ASC") fun getAllBooks(): Flow<List<BookEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertBook(book: BookEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertBooks(books: List<BookEntity>)

  @Query("DELETE FROM books WHERE id = :bookId AND userId = :userId")
  suspend fun deleteBook(bookId: String, userId: String)

  @Query("DELETE FROM books WHERE userId = :userId") suspend fun clearUserBooks(userId: String)

  @Query("DELETE FROM books") suspend fun clearAllBooks()

  @Query("SELECT EXISTS(SELECT 1 FROM books WHERE id = :bookId AND userId = :userId)")
  suspend fun bookExists(bookId: String, userId: String): Boolean

  @Query("SELECT COUNT(*) FROM books WHERE userId = :userId")
  fun getUserBookCount(userId: String): Flow<Int>
}
