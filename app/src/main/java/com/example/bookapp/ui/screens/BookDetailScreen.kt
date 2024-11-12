package com.example.bookapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bookapp.domain.model.Book

// Book detail screen
@Composable
fun BookDetailScreen(
    book: Book,
    onBackClick: () -> Unit,
    onSaveClick: (Book) -> Unit = {} // Add this parameter
) {
  Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
    // Buttons row
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
          Button(onClick = onBackClick) { Text("Back to Search") }

          Button(onClick = { onSaveClick(book) }) { Text("Save Book") }
        }

    // temporary Text to verify ID
    Text(
        text = "Book ID: ${book.id}",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(bottom = 8.dp))

    // Book details
    Text(
        text = book.title,
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.padding(bottom = 8.dp))

    Text(
        text = "By ${book.author}",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 16.dp))

    if (book.publishedDate.isNotBlank()) {
      Text(
          text = "Published: ${book.publishedDate}",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(bottom = 8.dp))
    }

    if (book.pageCount > 0) {
      Text(
          text = "Pages: ${book.pageCount}",
          style = MaterialTheme.typography.bodyMedium,
          modifier = Modifier.padding(bottom = 8.dp))
    }

    if (book.categories.isNotEmpty()) {
      Text(
          text = "Categories:",
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
          modifier = Modifier.padding(bottom = 4.dp))
      book.categories.forEach { category ->
        Text(
            text = "• $category",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(start = 8.dp, bottom = 2.dp))
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    if (book.description.isNotBlank()) {
      Text(
          text = "Description:",
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.padding(bottom = 8.dp))
      Text(text = book.description, style = MaterialTheme.typography.bodyMedium)
    }
  }
}
