package com.example.bookapp.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bookapp.domain.model.Book

@Composable
fun BookSearchScreen(
    viewModel: BookSearchViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    // ADDED: Show detail screen if book is selected
    state.selectedBook?.let { book ->
        BookDetailScreen(
            book = book,
            onBackClick = viewModel::onBackClick
        )
        return
    }

    // EXISTING SEARCH UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TextField(
            value = state.searchQuery,
            onValueChange = viewModel::onSearchQueryChange,
            label = { Text("Search for books") },
            placeholder = { Text("Enter book title") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
        )

        Button(
            onClick = viewModel::onSearchClick,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Search")
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp)
        ) {
            items(state.books) { book ->
                BookItem(
                    book = book,
                    // ADDED: onClick handler
                    onClick = { viewModel.onBookClick(book) }
                )
            }
        }
    }
}

// onClick parameter to BookItem
@Composable
fun BookItem(
    book: Book,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            // card clickable
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
        ) {
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "By ${book.author}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}