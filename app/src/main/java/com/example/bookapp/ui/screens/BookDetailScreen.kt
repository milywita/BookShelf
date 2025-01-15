// BookDetailScreen.kt
/**
 * Composable that displays detailed information about a book, including its title, author,
 * publication details, and description. Provides options to navigate back or save the book.
 */
package com.example.bookapp.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.bookapp.domain.model.Book
import com.example.bookapp.domain.model.BookGroup

@Composable
fun BookDetailScreen(
    book: Book,
    onBackClick: () -> Unit,
    onSaveClick: (Book) -> Unit = {},
    onDeleteClick: (String) -> Unit = {},
    isBookSaved: Boolean
) {
  var showGroupDialog by remember { mutableStateOf(false) }

  Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
    // Navigation and action buttons
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween) {
          Button(onClick = onBackClick) { Text("Back to Search") }
          if (isBookSaved) {
            Button(
                onClick = { onDeleteClick(book.id) },
                colors =
                    ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) {
                  Text("Delete Book")
                }
          } else {
            Button(onClick = { onSaveClick(book) }) { Text("Save Book") }
          }
        }

    Text(
        text = "Book ID: ${book.id}",
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(bottom = 8.dp))

    // Book details layout
    Text(
        text = book.title,
        style = MaterialTheme.typography.headlineMedium,
        modifier = Modifier.padding(bottom = 8.dp))

    Text(
        text = "By ${book.author}",
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.padding(bottom = 16.dp))

    // Group section
    if (isBookSaved) {
      OutlinedCard(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
              text = "Reading Status",
              style = MaterialTheme.typography.titleMedium,
              modifier = Modifier.padding(bottom = 8.dp))
          Text(
              text = book.group.displayName,
              style = MaterialTheme.typography.bodyMedium,
              modifier = Modifier.padding(bottom = 8.dp))
          Button(onClick = { showGroupDialog = true }, modifier = Modifier.fillMaxWidth()) {
            Text("Change Status")
          }
        }
      }
    }

    // Rest of the book details
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

  if (showGroupDialog) {
    GroupSelectionDialog(
        currentGroup = book.group,
        onDismiss = { showGroupDialog = false },
        onGroupSelected = { selectedGroup ->
          onSaveClick(book.copy(group = selectedGroup))
          showGroupDialog = false
        })
  }
}

@Composable
private fun GroupSelectionDialog(
    currentGroup: BookGroup,
    onDismiss: () -> Unit,
    onGroupSelected: (BookGroup) -> Unit
) {
  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text("Select Reading Status") },
      text = {
        Column {
          BookGroup.values().forEach { group ->
            RadioButton(
                selected = group == currentGroup,
                onClick = { onGroupSelected(group) },
                modifier = Modifier.padding(vertical = 4.dp))
            Text(
                text = group.displayName,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp))
          }
        }
      },
      confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } })
}
