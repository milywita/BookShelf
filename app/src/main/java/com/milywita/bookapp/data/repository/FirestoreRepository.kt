// FirestoreRepository.kt

package com.milywita.bookapp.data.repository

import android.util.Log
import com.milywita.bookapp.data.model.firebase.FirebaseBook
import com.milywita.bookapp.domain.error.AppError
import com.milywita.bookapp.domain.model.Book
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.trySendBlocking
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreRepository {
  companion object {
    private const val TAG = "FirestoreRepository"
  }

  private val firestore = FirebaseFirestore.getInstance()
  private val auth = FirebaseAuth.getInstance()

  private val currentUserId: String?
    get() = auth.currentUser?.uid

  private fun getUserBookCollection() =
      firestore
          .collection("users")
          .document(currentUserId ?: throw AppError.Auth.UserNotFound())
          .collection("books")

  suspend fun saveBook(book: Book) {
    Log.d(TAG, "Attempting to save book: ${book.id}")
    try {
      val firebaseBook = FirebaseBook.fromBook(book)
      getUserBookCollection().document(book.id).set(firebaseBook).await()
      Log.i(TAG, "Successfully saved book: ${book.id}")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to save book: ${book.id}", e)
      throw when (e) {
        is FirebaseFirestoreException ->
            when (e.code) {
              FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                  AppError.Firestore.PermissionDenied(cause = e)
              else -> AppError.Firestore.WriteError(documentId = book.id, cause = e)
            }
        else -> AppError.Unexpected(cause = e)
      }
    }
  }

  suspend fun getBook(bookId: String): FirebaseBook? {
    Log.d(TAG, "Attempting to fetch book: $bookId")
    try {
      return getUserBookCollection()
          .document(bookId)
          .get()
          .await()
          .toObject(FirebaseBook::class.java)
          ?.also { Log.i(TAG, "Successfully retrieved book: $bookId") }
    } catch (e: Exception) {
      Log.e(TAG, "Failed to get book: $bookId", e)
      throw when (e) {
        is FirebaseFirestoreException ->
            when (e.code) {
              FirebaseFirestoreException.Code.NOT_FOUND ->
                  AppError.Firestore.DocumentNotFound(documentId = bookId, cause = e)
              FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                  AppError.Firestore.PermissionDenied(cause = e)
              else -> AppError.Firestore.ReadError(documentId = bookId, cause = e)
            }
        else -> AppError.Unexpected(cause = e)
      }
    }
  }

  fun getUserBookStream(): Flow<List<FirebaseBook>> = callbackFlow {
    Log.d(TAG, "Starting book stream")
    val subscription =
        getUserBookCollection().addSnapshotListener { snapshot, error ->
          when {
            error != null -> {
              Log.e(TAG, "Error in book stream", error)
              trySendBlocking(emptyList())
            }
            snapshot != null -> {
              val books = snapshot.documents.mapNotNull { it.toObject(FirebaseBook::class.java) }
              Log.d(TAG, "Stream received ${books.size} books")
              trySendBlocking(books)
            }
          }
        }

    awaitClose {
      Log.d(TAG, "Closing book stream")
      subscription.remove()
    }
  }

  suspend fun deleteBook(bookId: String) {
    Log.d(TAG, "Attempting to delete book: $bookId")
    try {
      getUserBookCollection().document(bookId).delete().await()
      Log.i(TAG, "Successfully deleted book: $bookId")
    } catch (e: Exception) {
      Log.e(TAG, "Failed to delete book: $bookId", e)
      throw when (e) {
        is FirebaseFirestoreException ->
            when (e.code) {
              FirebaseFirestoreException.Code.NOT_FOUND ->
                  AppError.Firestore.DocumentNotFound(documentId = bookId, cause = e)
              FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                  AppError.Firestore.PermissionDenied(cause = e)
              else -> AppError.Firestore.WriteError(documentId = bookId, cause = e)
            }
        else -> AppError.Unexpected(cause = e)
      }
    }
  }
}
