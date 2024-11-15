package com.example.bookapp.ui.screens

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.assertIsDisplayed
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.bookapp.domain.model.Book
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookDetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var testBook: Book

    @Before
    fun setUp() {
        testBook = Book(
            id = "test_id",
            title = "Test Book",
            author = "Test Author",
            description = "Test Description",
            thumbnailUrl = "test_url",
            publishedDate = "2024",
            pageCount = 100,
            categories = listOf("Fiction", "Drama")
        )
    }

    @Test
    fun bookTitleIsDisplayed() {
        composeRule.setContent {
            BookDetailScreen(
                book = testBook,
                onBackClick = {},
                onSaveClick = {}
            )
        }

        composeRule.onNodeWithText(testBook.title).assertIsDisplayed()
    }
}