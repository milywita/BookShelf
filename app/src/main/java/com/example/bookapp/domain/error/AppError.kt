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
    }
    // Unexpected Errors
    data class Unexpected(
        override val message: String = "An unexpected error occurred",
        override val cause: Throwable? = null
    ) : AppError()
}
