// BookSearchScreen.kt
package com.example.bookapp.ui.screens

import android.app.Application
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.bookapp.domain.model.Book
import com.example.bookapp.BuildConfig
import com.example.bookapp.domain.model.BookGroup
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import coil.compose.AsyncImage
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.getValue


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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = if (state.isShowingSavedBooks) "My Library" else "Search Books",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                if (state.isShowingSavedBooks) {
                    Text(
                        text = "${savedBooks.size} books saved",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
            Button(
                onClick = viewModel::toggleSavedBooks,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(if (state.isShowingSavedBooks) "Search Books" else "View Library")
            }
        }
    }

    if (state.isShowingSavedBooks) {
        SavedBooksList(savedBooks, viewModel::onBookClick)
    } else {
        SearchSection(state, viewModel)
    }
}
@Composable
private fun SavedBooksList(
    books: List<Book>,
    onBookClick: (Book) -> Unit
) {
    if (books.isEmpty()) {
        EmptyLibraryMessage()
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    ) {
        // Header section
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "My Library",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "${books.size} books saved",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Books by group
        val groupedBooks = books.groupBy { it.group }
            .filter { it.key != BookGroup.NONE }
            .toSortedMap(compareBy { it.ordinal })

        // Display grouped books
        groupedBooks.forEach { (group, booksInGroup) ->
            item {
                Text(
                    text = group.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(booksInGroup) { book ->
                BookItem(book = book, onClick = { onBookClick(book) })
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }

        // Ungrouped books
        val ungroupedBooks = books.filter { it.group == BookGroup.NONE }
        if (ungroupedBooks.isNotEmpty()) {
            item {
                Text(
                    text = "Other Books",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(ungroupedBooks) { book ->
                BookItem(book = book, onClick = { onBookClick(book) })
            }
        }
    }
}

@Composable
private fun EmptyLibraryMessage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.MenuBook,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Your library is empty",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
        Text(
            text = "Save books to see them here",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        )
    }
}

@Composable
private fun SearchSection(
    state: SearchState,
    viewModel: BookSearchViewModel
) {
    Column {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = viewModel::onSearchQueryChange,
            label = { Text("Search for books") },
            placeholder = { Text("Enter book title, author, or keyword") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            singleLine = true,
            trailingIcon = {
                IconButton(
                    onClick = viewModel::onSearchClick,
                    enabled = !state.isLoading && state.searchQuery.isNotEmpty()
                ) {
                    Icon(Icons.Default.Search, contentDescription = "Search")
                }
            }
        )

        if (state.isLoading) {
            LoadingAnimation()
        } else if (state.books.isEmpty() && state.searchQuery.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Search for books",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Text(
                    text = "Find books by title, author, or keyword",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            ) {
                if (state.books.isNotEmpty()) {
                    item {
                        Text(
                            text = "Search Results (${state.books.size})",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                }

                items(
                    items = state.books,
                    key = { book -> book.id }
                ) { book ->
                    BookItem(book = book, onClick = { viewModel.onBookClick(book) })
                }
            }
        }
    }
}
@Composable
private fun BookItem(
    book: Book,
    onClick: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable(onClick = onClick)
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        BookItemContent(book = book, expanded = expanded, onExpandToggle = { expanded = it })
    }
}

@Composable
private fun BookItemContent(
    book: Book,
    expanded: Boolean,
    onExpandToggle: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BookCover(book.thumbnailUrl, book.title)
        BookInfo(
            book = book,
            expanded = expanded,
            onExpandToggle = onExpandToggle,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun BookCover(thumbnailUrl: String, title: String) {
    Box(
        modifier = Modifier
            .width(80.dp)
            .height(120.dp)
            .clip(RoundedCornerShape(8.dp))
    ) {
        var imageLoading by remember { mutableStateOf(true) }

        if (imageLoading) {
            CoverShimmer()
        }

        AsyncImage(
            model = thumbnailUrl.ifEmpty {
                "https://via.placeholder.com/128x192.png?text=No+Cover"
            },
            contentDescription = "Book cover for $title",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            onLoading = { imageLoading = true },
            onSuccess = { imageLoading = false },
            onError = { imageLoading = false }
        )
    }
}

@Composable
private fun CoverShimmer() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    start = Offset(0f, 0f),
                    end = Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                )
            )
    )
}

@Composable
private fun BookInfo(
    book: Book,
    expanded: Boolean,
    onExpandToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        BookBasicInfo(book, expanded)

        if (book.group != BookGroup.NONE) {
            BookGroupChip(group = book.group)
        }

        BookCategory(book.categories)
        BookDescription(
            description = book.description,
            expanded = expanded,
            onExpandToggle = onExpandToggle
        )
    }
}

@Composable
private fun BookBasicInfo(book: Book, expanded: Boolean) {
    Text(
        text = book.title,
        style = MaterialTheme.typography.titleMedium,
        maxLines = if (expanded) Int.MAX_VALUE else 2,
        overflow = TextOverflow.Ellipsis
    )

    Text(
        text = "By ${book.author}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun BookCategory(categories: List<String>) {
    if (categories.isNotEmpty()) {
        Text(
            text = categories.first(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun BookDescription(
    description: String,
    expanded: Boolean,
    onExpandToggle: (Boolean) -> Unit
) {
    if (description.isNotEmpty()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = { onExpandToggle(!expanded) }
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Default.KeyboardArrowUp
                    else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Show less" else "Show more"
                )
            }
        }

        AnimatedVisibility(visible = expanded) {
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
private fun BookGroupChip(group: BookGroup) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = when (group) {
            BookGroup.READING -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            BookGroup.WANT_TO_READ -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.1f)
            BookGroup.FINISHED -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
            BookGroup.DID_NOT_FINISH -> MaterialTheme.colorScheme.error.copy(alpha = 0.1f)
            BookGroup.NONE -> MaterialTheme.colorScheme.surfaceVariant
        },
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = group.displayName,
            style = MaterialTheme.typography.bodySmall,
            color = when (group) {
                BookGroup.READING -> MaterialTheme.colorScheme.primary
                BookGroup.WANT_TO_READ -> MaterialTheme.colorScheme.secondary
                BookGroup.FINISHED -> MaterialTheme.colorScheme.tertiary
                BookGroup.DID_NOT_FINISH -> MaterialTheme.colorScheme.error
                BookGroup.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun LoadingAnimation(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "loading")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1000
                0.7f at 500
                0.9f at 800
            },
            repeatMode = RepeatMode.Reverse
        ),
        label = "loading"
    )

    Row(
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            color = MaterialTheme.colorScheme.primary,
            strokeWidth = 2.dp
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Searching...",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary.copy(alpha = alpha)
        )
    }
}