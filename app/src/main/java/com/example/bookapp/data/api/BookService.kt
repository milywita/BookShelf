package com.example.bookapp.data.api

import com.example.bookapp.data.model.BookResponse
import retrofit2.http.GET
import retrofit2.http.Query

// Retrofit API interface
interface BookService {
    @GET("volumes")
    suspend fun searchBooks(
        @Query("q") query: String,
        @Query("maxResults") maxResult: Int = 40
    ): BookResponse


}
