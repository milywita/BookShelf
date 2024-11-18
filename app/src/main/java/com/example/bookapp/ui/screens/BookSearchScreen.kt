// BookSearchScreen.kt
/**
 * Main screen for book search functionality. Provides search interface and displays both search
 * results and saved books.
 */
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookSearchScreen(
    onSignOut: () -> Unit = {},
    viewModel: BookSearchViewModel =
        viewModel(
            factory =
                BookSearchViewModelFactory.provide(
                    LocalContext.current.applicationContext as Application))
) {
  val context = LocalContext.current
  val state by viewModel.state.collectAsState()
  val savedBooks by viewModel.savedBooks.collectAsState()

  // Message display handling using Toast
  LaunchedEffect(state.message) {
    state.message?.let { message ->
      Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
      viewModel.clearMessage()
    }
  }

  state.selectedBook?.let { book ->
    BookDetailScreen(
        book = book, onBackClick = viewModel::onBackClick, onSaveClick = viewModel::saveBook)
    return
  }

  Scaffold(
      topBar = {
        TopAppBar(
            title = { Text("Book Search") },
            actions = {
              IconButton(onClick = onSignOut) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Sign Out")
              }
            },
            colors =
                TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer))
      }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
          Button(
              onClick = viewModel::toggleSavedBooks,
              modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                Text(if (state.isShowingSavedBooks) "Show Search" else "Show Saved Books")
              }

          if (state.isShowingSavedBooks) {
            LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
              items(savedBooks) { book ->
                BookItem(book = book, onClick = { viewModel.onBookClick(book) })
              }
            }
          } else {
            TextField(
                value = state.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                label = { Text("Search for books") },
                placeholder = { Text("Enter book title") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp))

            Button(onClick = viewModel::onSearchClick, modifier = Modifier.fillMaxWidth()) {
              Text("Search")
            }

            LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
              items(state.books) { book ->
                BookItem(book = book, onClick = { viewModel.onBookClick(book) })
              }
            }
          }
        }
      }
}

/**
 * Reusable composable for displaying individual book items in lists.
 */

@Composable
fun BookItem(book: Book, onClick: () -> Unit) {
  Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable(onClick = onClick)) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(text = book.title, style = MaterialTheme.typography.titleMedium)
      Text(text = "By ${book.author}", style = MaterialTheme.typography.bodyMedium)
    }
  }
}
