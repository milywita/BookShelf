package com.example.bookapp.ui.screens

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bookapp.domain.model.Book

// Main search screen with saved books toggle
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSearchScreen(
    // Callback function for sign out action, defaults to empty function
    onSignOut: () -> Unit = {},
    // Initialize ViewModel with custom factory
    viewModel: BookSearchViewModel = viewModel(
        factory = BookSearchViewModelFactory.provide(LocalContext.current.applicationContext as Application)
    )
) {
    // Get current Android context for Toast messages
    val context = LocalContext.current
    // Collect UI state from ViewModel as State<SearchState>
    val state by viewModel.state.collectAsState()
    // Collect saved books as State<List<Book>>
    val savedBooks by viewModel.savedBooks.collectAsState()

    // Side effect to show toast messages when they change
    LaunchedEffect(state.message) {
        state.message?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    // If a book is selected, show the detail screen
    state.selectedBook?.let { book ->
        BookDetailScreen(
            book = book,
            onBackClick = viewModel::onBackClick,
            onSaveClick = viewModel::saveBook
        )
        return
    }

    // Main screen scaffold with top app bar
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Search") },
                // Add sign out button to app bar
                actions = {
                    IconButton(onClick = onSignOut) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Sign Out"
                        )
                    }
                },
                // Style the app bar using Material3 colors
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        // Main content column
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding) // Apply scaffold padding
                .padding(16.dp)   // Add additional padding for content
        ) {
            // Toggle button to switch between search and saved books
            Button(
                onClick = viewModel::toggleSavedBooks,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
            ) {
                Text(if (state.isShowingSavedBooks) "Show Search" else "Show Saved Books")
            }

            // Conditional UI based on whether showing saved books or search
            if (state.isShowingSavedBooks) {
                // Display saved books in a scrollable list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    items(savedBooks) { book ->
                        BookItem(book = book, onClick = { viewModel.onBookClick(book) })
                    }
                }
            } else {
                // Search interface
                // Search input field
                TextField(
                    value = state.searchQuery,
                    onValueChange = viewModel::onSearchQueryChange,
                    label = { Text("Search for books") },
                    placeholder = { Text("Enter book title") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                )

                // Search button
                Button(
                    onClick = viewModel::onSearchClick,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Search")
                }

                // Search results list
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    items(state.books) { book ->
                        BookItem(book = book, onClick = { viewModel.onBookClick(book) })
                    }
                }
            }
        }
    }
}

// Reusable composable for displaying individual book items
@Composable
fun BookItem(
    book: Book,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick) // Make entire card clickable
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Book title with medium emphasis
            Text(
                text = book.title,
                style = MaterialTheme.typography.titleMedium
            )
            // Author name with less emphasis
            Text(
                text = "By ${book.author}",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}