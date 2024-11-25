// BookSyncRepository.kt
/**
 * Repository that handles synchronization between local Room database and Firebase Firestore.
 * Provides a single source of truth for book data while maintaining offline capabilities.
 */
package com.example.bookapp.data.repository

import com.example.bookapp.data.local.dao.BookDao
import com.example.bookapp.domain.model.Book
import com.example.bookapp.data.local.entity.toBook
import com.example.bookapp.data.local.entity.toBookEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first

class BookSyncRepository(
    private val bookDao: BookDao,
    private val firestoreRepository: FirestoreRepository
) {
    fun getSavedBooks(): Flow<List<Book>>{
        return bookDao.getAllBooks()
            .map{ entities -> entities.map {it.toBook()}}
            .catch {e ->
                e.printStackTrace()
                emit(emptyList())
            }
    }

    suspend fun saveBook(book: Book) {
        try {
            bookDao.insertBook(book.toBookEntity())

            firestoreRepository.saveBook(book)
        } catch (e: Exception) {
            e.printStackTrace()
            //TODO: Add to a retry queue here for later sync
        }
    }

    suspend fun syncWithFirestore() {
        try {
            val firestoreBooks = firestoreRepository.getUserBookStream().first()

            firestoreBooks.forEach { firebaseBook ->
                bookDao.insertBook(firebaseBook.toBook().toBookEntity())
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // TODO: Trigger retry mechanism or notify user of sync failure
        }
    }

    suspend fun updateBookProgress(bookId: String, progress: Int) {
        try {
            firestoreRepository.updateBookProgress(bookId, progress)
            // TODO: Update local entity if progress field is added to Room
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun toggleBookLike(bookId: String, isLiked: Boolean) {
        try {
            firestoreRepository.toggleBookLike(bookId, isLiked)
            // TODO: Update local entity if isLiked field is added to Room
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateNotes(bookId: String, notes: String) {
        try {
            firestoreRepository.updateNotes(bookId, notes)
            // TODO: Update local entity if notes field is added to Room
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun deleteBook(bookId: String) {
        try {
            bookDao.deleteBook(bookId)
            firestoreRepository.deleteBook(bookId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
