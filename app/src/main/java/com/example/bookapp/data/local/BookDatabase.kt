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

@Database(entities = [BookEntity::class], version = 2, exportSchema = true)

abstract class BookDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao

    companion object {
        @Volatile
        private var INSTANCE: BookDatabase? = null
        private const val DATABASE_NAME = "book_database"


        fun getDatabase(context: Context): BookDatabase {
            return INSTANCE ?: synchronized(this) {
                createDatabase(context).also { INSTANCE = it }
            }
        }

        private fun createDatabase(context: Context) =
            Room.databaseBuilder(
                context.applicationContext,
                BookDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
    }
}