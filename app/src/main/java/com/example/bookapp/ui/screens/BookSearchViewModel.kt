package com.example.bookapp.ui.screens

import androidx.lifecycle.ViewModel
import com.example.bookapp.domain.model.Book
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BookSearchViewModel : ViewModel(){
    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    fun onSearchQueryChange(query: String){
        _state.value = _state.value.copy(searchQuery = query)
    }

    fun onSearchClick(){
        val sampleBooks = listOf(
            Book("The Great Gatsby", "F. Scott Fitzgerald"),
            Book("1984", "George Orwell"),
            Book("Pride and Prejudice", "Jane Austen")
        )
        _state.value = _state.value.copy(books = sampleBooks)
    }
}

