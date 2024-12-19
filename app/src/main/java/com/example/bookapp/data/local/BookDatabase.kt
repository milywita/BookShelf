// BookDatabase.kt
/**
 * Room database implementation for local book storage. Provides data persistence and offline access
 * capability.
 */
package com.example.bookapp.data.local

import android.content.Context
import android.util.Log
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.bookapp.data.local.dao.BookDao
import com.example.bookapp.data.local.entity.BookEntity
import com.example.bookapp.domain.error.AppError

@Database(entities = [BookEntity::class], version = 2, exportSchema = true)
abstract class BookDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao

    companion object {
        private const val TAG = "BookDatabase"
        private const val DATABASE_NAME = "book_database"

        @Volatile
        private var INSTANCE: BookDatabase? = null

        fun getDatabase(context: Context): BookDatabase {
            Log.d(TAG, "Getting database instance")
            return INSTANCE ?: synchronized(this) {
                try {
                    createDatabase(context).also {
                        INSTANCE = it
                        Log.i(TAG, "Database instance created successfully")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to get database instance", e)
                    throw AppError.Database.WriteError(
                        message = "Failed to initialize database",
                        cause = e
                    )
                }
            }
        }

        private fun createDatabase(context: Context) = try {
            Log.d(TAG, "Creating database: $DATABASE_NAME")
            Room.databaseBuilder(
                context.applicationContext,
                BookDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
                .also { Log.i(TAG, "Database created successfully") }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create database", e)
            throw AppError.Database.WriteError(
                message = "Failed to create database: ${e.message}",
                cause = e
            )
        }
    }
}