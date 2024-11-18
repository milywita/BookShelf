// NetworkModule.kt
/**
 * Dependency injection module for networking components.
 * Configures and provides Retrofit client with Moshi JSON parsing.
 */
package com.example.bookapp.data.di

import com.example.bookapp.data.api.BookService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object NetworkModule {
  private const val BASE_URL = "https://www.googleapis.com/books/v1/"

  private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

  private val retrofit =
      Retrofit.Builder()
          .baseUrl(BASE_URL)
          .addConverterFactory(MoshiConverterFactory.create(moshi))
          .build()

  val bookService: BookService = retrofit.create(BookService::class.java)
}
