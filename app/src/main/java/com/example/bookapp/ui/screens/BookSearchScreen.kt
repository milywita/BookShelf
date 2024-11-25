package com.example.bookapp.ui.screens

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bookapp.domain.model.Book
import firebase.com.protolitewrapper.BuildConfig

@Composable
fun BookSearchScreen(
    onSignOut: () -> Unit = {},
    viewModel: BookSearchViewModel = viewModel(
        factory = BookSearchViewModelFactory.provide(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val savedBooks by viewModel.savedBooks.collectAsState()
    val localBooksExist by viewModel.localBooksExist.collectAsState()
    val debugInfo by viewModel.debugInfo.collectAsState()

    LaunchedEffect(state.message) {
        state.message?.let { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
            viewModel.clearMessage()
        }
    }

    state.selectedBook?.let { book ->
        BookDetailScreen(
            book = book,
            onBackClick = viewModel::onBackClick,
            onSaveClick = viewModel::saveBook,
            onDeleteClick = viewModel::deleteBook,
            isBookSaved = viewModel.isBookSaved(book.id)
        )
        return
    }

    MainScreenContent(
        state = state,
        debugInfo = debugInfo,
        localBooksExist = localBooksExist,
        savedBooks = savedBooks,
        viewModel = viewModel,
        onSignOut = onSignOut
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreenContent(
    state: SearchState,
    debugInfo: BookSearchViewModel.DebugInfo,
    localBooksExist: Boolean,
    savedBooks: List<Book>,
    viewModel: BookSearchViewModel,
    onSignOut: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book Search") },
                actions = {
                    if (BuildConfig.DEBUG) {
                        DebugButton(viewModel::toggleDebugInfo)
                    }
                    SignOutButton(onSignOut)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            if (debugInfo.isVisible) {
                DebugPanel(debugInfo, viewModel::forceCheckLocalBooks)
            }

            if (localBooksExist) {
                MigrationCard(viewModel::migrateLocalBooks)
            }

            BookListContent(
                state = state,
                savedBooks = savedBooks,
                viewModel = viewModel
            )
        }
    }
}

@Composable
private fun DebugButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Default.Build,
            contentDescription = "Debug Info"
        )
    }
}

@Composable
private fun SignOutButton(onClick: () -> Unit) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
            contentDescription = "Sign Out"
        )
    }
}

@Composable
private fun DebugPanel(
    debugInfo: BookSearchViewModel.DebugInfo,
    onCheckBooks: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Debug Information",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Text("Current User ID: ${debugInfo.currentUserId}")
            Text("Local Books: ${debugInfo.localBookIds.joinToString()}")
            Text("Firestore Books: ${debugInfo.firestoreBookIds.joinToString()}")
            Text("Local-only Books: ${debugInfo.localOnlyBookIds.joinToString()}")

            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onCheckBooks) {
                Text("Force Check Local Books")
            }
        }
    }
}

@Composable
private fun MigrationCard(onMigrate: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Local Books Available to Import",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                )
            )
            Text(
                "There are books saved locally that aren't in your cloud library. " +
                        "Would you like to import them to your account?",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Button(
                onClick = onMigrate,
                modifier = Modifier.align(Alignment.End),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Import Books")
            }
        }
    }
}

@Composable
private fun BookListContent(
    state: SearchState,
    savedBooks: List<Book>,
    viewModel: BookSearchViewModel
) {
    Column {
        Button(
            onClick = viewModel::toggleSavedBooks,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.isShowingSavedBooks) "Show Search" else "Show Saved Books")
        }

        if (state.isShowingSavedBooks) {
            SavedBooksList(savedBooks, viewModel::onBookClick)
        } else {
            SearchSection(state, viewModel)
        }
    }
}

@Composable
private fun SavedBooksList(
    books: List<Book>,
    onBookClick: (Book) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp)
    ) {
        items(
            items = books,
            key = { book -> book.id }
        ) { book ->
            BookItem(book = book, onClick = { onBookClick(book) })
        }
    }
}

@Composable
private fun SearchSection(
    state: SearchState,
    viewModel: BookSearchViewModel
) {
    OutlinedTextField(
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
        items(
            items = state.books,
            key = { book -> book.id }
        ) { book ->
            BookItem(book = book, onClick = { viewModel.onBookClick(book) })
        }
    }
}

@Composable
private fun BookItem(
    book: Book,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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