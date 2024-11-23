package com.example.bookapp.data.repository

import com.example.bookapp.data.model.firebase.FirebaseBook
import com.example.bookapp.domain.model.Book
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
  private val firestore = FirebaseFirestore.getInstance()
  private val auth = FirebaseAuth.getInstance()

  private val currentUserId: String?
    get() = auth.currentUser?.uid

  private fun getUserBookCollection() =
      firestore
          .collection("users")
          .document(currentUserId ?: throw IllegalArgumentException("User not logged in"))
          .collection("books")

  suspend fun saveBook(book: Book) {
    try {
      val firebaseBook = FirebaseBook.fromBook(book)

      getUserBookCollection().document(book.id).set(firebaseBook).await()
    } catch (e: Exception) {
      throw Exception("Failed to save book ${e.message}")
    }
  }

  // Specific book
  suspend fun getBook(bookId: String): FirebaseBook? {
    return try {
      getUserBookCollection().document(bookId).get().await().toObject(FirebaseBook::class.java)
    } catch (e: Exception) {
      throw Exception("Failed to get book: ${e.message}")
    }
  }

  fun getUserBookStream(): Flow<List<FirebaseBook>> = callbackFlow {
    val subscription =
        getUserBookCollection().addSnapshotListener { snapshot, error ->
          if (error != null) {
            trySendBlocking(emptyList())
            return@addSnapshotListener
          }

          val books =
              snapshot?.documents?.mapNotNull { it.toObject(FirebaseBook::class.java) }
                  ?: emptyList()

          trySendBlocking(books)
        }

    awaitClose { subscription.remove() }
  }

  suspend fun updateBookProgress(bookId: String, progress: Int) {
    require(progress in 0..100) { "Progress must be between 0 and 100" }

    try {
      getUserBookCollection().document(bookId).update("readingProgress", progress).await()
    } catch (e: Exception) {
      throw Exception("Failed to update reading progress: ${e.message}")
    }
  }

  suspend fun toggleBookLike(bookId: String, isLiked: Boolean) {
    try {
      getUserBookCollection().document(bookId).update("isLiked", isLiked).await()
    } catch (e: Exception) {
      throw Exception("Failed to update like status: ${e.message}")
    }
  }

  suspend fun updateNotes(bookId: String, notes: String) {
    try {
      getUserBookCollection().document(bookId).update("notes", notes).await()
    } catch (e: Exception) {
      throw Exception("Failed to update notes: ${e.message}")
    }
  }

  suspend fun deleteBook(bookId: String) {
    try {
      getUserBookCollection().document(bookId).delete().await()
    } catch (e: Exception) {
      throw Exception("Failed to delete book: ${e.message}")
    }
  }
}
