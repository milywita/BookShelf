package com.example.bookapp.ui.screens

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bookapp.domain.model.Book

// Main search screen with saved books toggle

@Composable
fun BookSearchScreen() {
  val context = LocalContext.current
  val viewModel: BookSearchViewModel =
      viewModel(
          factory =
              object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                  if (modelClass.isAssignableFrom(BookSearchViewModel::class.java)) {
                    return BookSearchViewModel(context.applicationContext as Application) as T
                  }
                  throw IllegalArgumentException("Unknown ViewModel class")
                }
              })

  val state by viewModel.state.collectAsState()
  val savedBooks by viewModel.savedBooks.collectAsState()

  // Show toast when message changes
  LaunchedEffect(state.message) {
    state.message?.let { message ->
      Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
      viewModel.clearMessage()
    }
  }

  // Handle book detail navigation
  state.selectedBook?.let { book ->
    BookDetailScreen(
        book = book, onBackClick = viewModel::onBackClick, onSaveClick = viewModel::saveBook)
    return
  }

  Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
    // Toggle between search and saved books
    Button(
        onClick = viewModel::toggleSavedBooks,
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
          Text(if (state.isShowingSavedBooks) "Show Search" else "Show Saved Books")
        }

    if (state.isShowingSavedBooks) {
      // Saved books list
      LazyColumn(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
        items(savedBooks) { book ->
          BookItem(book = book, onClick = { viewModel.onBookClick(book) })
        }
      }
    } else {
      // Show search interface and results
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

@Composable
fun BookItem(book: Book, onClick: () -> Unit) {
  Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).clickable(onClick = onClick)) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(text = book.title, style = MaterialTheme.typography.titleMedium)
      Text(text = "By ${book.author}", style = MaterialTheme.typography.bodyMedium)
    }
  }
}
