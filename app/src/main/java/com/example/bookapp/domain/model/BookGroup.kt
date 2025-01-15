package com.example.bookapp.domain.model

enum class BookGroup(val displayName: String) {
    READING("Currently Reading"),
    WANT_TO_READ("Want to Read"),
    FINISHED("Finished Reading"),
    DID_NOT_FINISH("Did Not Finish"),
    NONE("No Group");

    companion object {
        fun fromString(value: String): BookGroup {
            return try {
                valueOf(value)
            } catch (e: IllegalArgumentException) {
                NONE
            }
        }
    }
}