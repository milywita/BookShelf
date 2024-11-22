// BookDatabase.kt
/**
 * Room database implementation for local book storage. Provides data persistence and offline access
 * capability.
 */
package com.example.bookapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.bookapp.data.local.dao.BookDao
import com.example.bookapp.data.local.entity.BookEntity

@Database(entities = [BookEntity::class], version = 1, exportSchema = true)
abstract class BookDatabase : RoomDatabase() {
  abstract fun bookDao(): BookDao

  companion object {
    @Volatile private var INSTANCE: BookDatabase? = null

    fun getDatabase(context: Context): BookDatabase {
      return INSTANCE
          ?: synchronized(this) {
            // Create database if it doesn't exist
            val instance =
                Room.databaseBuilder(
                        context.applicationContext, BookDatabase::class.java, "book_database")
                    .build()
            INSTANCE = instance
            instance
          }
    }
  }
}
