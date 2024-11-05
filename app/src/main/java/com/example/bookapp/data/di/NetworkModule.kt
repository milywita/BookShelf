package com.example.bookapp.data.di

import com.example.bookapp.data.api.BookService
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object NetworkModule{
    // Creating Moshi instance with Kotlin adapter
    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // Creating Retrofit instance
    private val retrofit = Retrofit.Builder()
        .baseUrl(BookService.BASE_URL)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    // Creating API service instance
    val bookService: BookService = retrofit.create(BookService::class.java)
}
