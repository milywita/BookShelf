//AppError.kt
package com.example.bookapp.domain.error

sealed class AppError : Exception() {
    // Authentication Errors
    sealed class Auth : AppError() {

        data class InvalidCredentials(
            override val message: String = "Incorrect email or password",
            override val cause: Throwable? = null
        ) : Auth()

        data class WeakPassword(
            override val message: String = "Password must be at least 6 characters",
            override val cause: Throwable? = null
        ) : Auth()

        data class EmailAlreadyInUse(
            override val message: String = "An account already exists with this email",
            override val cause: Throwable? = null
        ) : Auth()

        data class InvalidEmail(
            override val message: String = "Please enter a valid email address",
            override val cause: Throwable? = null
        ) : Auth()

        data class UserNotFound(
            override val message: String = "Account not found",
            override val cause: Throwable? = null
        ) : Auth()

        data class SignOutError(
            override val message: String = "Failed to sign out properly",
            override val cause: Throwable? = null
        ) : Auth()

        data class ValidationError(
            override val message: String = "Please check your input and try again",
            override val cause: Throwable? = null
        ) : Auth()

        data class SessionExpired(
            override val message: String = "Your session has expired. Please sign in again",
            override val cause: Throwable? = null
        ) : Auth()
    }
    // Book-related Errors
    sealed class Book : AppError() {

        data class SearchFailed(
            override val message: String = "Failed to search for books",
            override val cause: Throwable? = null
        ) : Book()

        data class DeleteFailed(
            override val message: String = "Failed to delete book",
            val bookId: String? = null,
            override val cause: Throwable? = null
        ) : Book()


    }
    // Network Errors
    sealed class Network : AppError() {

        data class NoConnection(
            override val message: String = "Please check your internet connection",
            override val cause: Throwable? = null
        ) : Network()

        data class ServerError(
            override val message: String = "Server error occurred",
            override val cause: Throwable? = null
        ) : Network()

        data class ServiceInitializationError(
            override val message: String = "Failed to initialize service",
            val serviceName: String? = null,
            override val cause: Throwable? = null
        ) : Network()

    }

    // Sync Errors
    sealed class Sync : AppError() {
        data class NetworkError(
            override val message: String = "Failed to sync with cloud storage",
            override val cause: Throwable? = null
        ) : Sync()

        data class AuthenticationError(
            override val message: String = "User not authenticated for sync operation",
            override val cause: Throwable? = null
        ) : Sync()

        data class ConflictError(
            override val message: String = "Data conflict during sync",
            val itemId: String? = null,
            override val cause: Throwable? = null
        ) : Sync()

        data class StorageError(
            override val message: String = "Local storage operation failed",
            override val cause: Throwable? = null
        ) : Sync()
    }

    // Firestore specific errors
    sealed class Firestore : AppError() {
        data class DocumentNotFound(
            override val message: String = "Document not found in Firestore",
            val documentId: String? = null,
            override val cause: Throwable? = null
        ) : Firestore()

        data class PermissionDenied(
            override val message: String = "Permission denied accessing Firestore",
            override val cause: Throwable? = null
        ) : Firestore()

        data class WriteError(
            override val message: String = "Failed to write to Firestore",
            val documentId: String? = null,
            override val cause: Throwable? = null
        ) : Firestore()

        data class ReadError(
            override val message: String = "Failed to read from Firestore",
            val documentId: String? = null,
            override val cause: Throwable? = null
        ) : Firestore()

    }

    //Database

    sealed class Database : AppError() {
        data class ReadError(
            override val message: String = "Failed to read from database",
            override val cause: Throwable? = null
        ) : Database()

        data class WriteError(
            override val message: String = "Failed to write to database",
            override val cause: Throwable? = null
        ) : Database()
    }

    // Unexpected Errors
    data class Unexpected(
        override val message: String = "An unexpected error occurred",
        override val cause: Throwable? = null
    ) : AppError()

}
