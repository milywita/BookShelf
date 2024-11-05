package com.example.bookapp.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bookapp.data.di.NetworkModule
import com.example.bookapp.data.repository.BookRepository
import com.example.bookapp.domain.model.Book
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.http.Query

class BookSearchViewModel : ViewModel (){
    private val repository = BookRepository(NetworkModule.bookService)
    private val _state = MutableStateFlow(SearchState())
    val state: StateFlow<SearchState> = _state.asStateFlow()

    fun onSearchQueryChange(query: String){
        _state.value = _state.value.copy(searchQuery = query)
    }

    fun onSearchClick(){
        val currentQuery = state.value.searchQuery
        if (currentQuery.isBlank()) return

        viewModelScope.launch {
            try {
                val books = repository.searchBooks(currentQuery)
                _state.value = _state.value.copy(books = books)
            } catch (e: Exception){
                e.printStackTrace()
                // add some error handling later
            }
        }
    }

    fun onBookClick(book: Book) {
        _state.value = _state.value.copy(selectedBook = book)
    }

    fun onBackClick() {
        _state.value = _state.value.copy(selectedBook = null)
    }

}