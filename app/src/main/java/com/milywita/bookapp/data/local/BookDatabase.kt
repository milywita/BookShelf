// BookDatabase.kt
package com.milywita.bookapp.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.milywita.bookapp.data.local.dao.BookDao
import com.milywita.bookapp.data.local.entity.BookEntity
import com.milywita.bookapp.domain.error.AppError
import com.milywita.bookapp.domain.model.BookGroup
import com.milywita.bookapp.util.Logger

@Database(entities = [BookEntity::class], version = 3, exportSchema = true)
abstract class BookDatabase : RoomDatabase() {
  abstract fun bookDao(): BookDao

  companion object {
    private const val TAG = "BookDatabase"
    private const val DATABASE_NAME = "book_database"

    private val MIGRATION_2_3 =
        object : Migration(2, 3) {
          override fun migrate(database: SupportSQLiteDatabase) {
            Logger.d(TAG, "Migrating database from version 2 to 3")
            database.execSQL(
                "ALTER TABLE books ADD COLUMN `group` TEXT NOT NULL DEFAULT '${BookGroup.NONE.name}'")
          }
        }

    @Volatile private var INSTANCE: BookDatabase? = null

    fun getDatabase(context: Context): BookDatabase {
      Logger.d(TAG, "Getting database instance")
      return INSTANCE
          ?: synchronized(this) {
            try {
              createDatabase(context).also {
                INSTANCE = it
                Logger.i(TAG, "Database instance created successfully")
              }
            } catch (e: Exception) {
              Logger.e(TAG, "Failed to get database instance", e)
              throw AppError.Database.WriteError(
                  message = "Failed to initialize database", cause = e)
            }
          }
    }

    private fun createDatabase(context: Context) =
        try {
          Logger.d(TAG, "Creating database: $DATABASE_NAME")
          Room.databaseBuilder(context.applicationContext, BookDatabase::class.java, DATABASE_NAME)
              .addMigrations(MIGRATION_2_3)
              .fallbackToDestructiveMigration()
              .build()
              .also { Logger.i(TAG, "Database created successfully") }
        } catch (e: Exception) {
          Logger.e(TAG, "Failed to create database", e)
          throw AppError.Database.WriteError(
              message = "Failed to create database: ${e.message}", cause = e)
        }
  }
}
