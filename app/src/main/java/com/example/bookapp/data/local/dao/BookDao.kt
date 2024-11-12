package com.example.bookapp.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.bookapp.data.local.entity.BookEntity
import kotlinx.coroutines.flow.Flow

// Data Access Object(DAO) -Defines database operations
@Dao
interface BookDao {
  @Query("SELECT * FROM books") fun getAllBooks(): Flow<List<BookEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertBook(book: BookEntity)
}
