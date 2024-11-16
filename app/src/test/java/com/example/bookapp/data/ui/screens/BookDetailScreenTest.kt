package com.example.bookapp.data.ui.screens

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.bookapp.domain.model.Book
import com.example.bookapp.ui.screens.BookDetailScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
/*
- Robolectic is a testing framework that run Android test on machine instead of a real device or emulator
- This makes test run much faster than on-device tests
 */
@Config(sdk = [34]) // Tells Robotic which Android SDK version to simulate
class BookDetailScreenTest {

  @get:Rule
  val composeTestRule =
      createComposeRule() // A test rule that helps set up the Jetpack Compose testing environment,
  // creates isolated environment for each test

  // Complete test book with all fields
  private val completeBook =
      Book(
          id = "test123",
          title = "Test Book Title",
          author = "Test Author",
          description = "Test description",
          publishedDate = "2024",
          pageCount = 200,
          categories = listOf("Fiction", "Mystery", "Thriller"),
          thumbnailUrl =
              "https://example.com/image.jpg") // Test data that will be used across multiple tests

  // Minimal test book with only required fields
  private val minimalBook =
      Book(
          id = "min123",
          title = "Minimal Book",
          author = "Unknown Author",
          description = "",
          publishedDate = "",
          pageCount = 0,
          categories = emptyList(),
          thumbnailUrl = "")

  @Test // Marks a method as a test case
  fun `verify basic book title is displayed`() {
    println("Starting test: verify basic book title is displayed")

    // Arrange, set up the test environment
    composeTestRule.setContent {
      BookDetailScreen(book = completeBook, onBackClick = {}, onSaveClick = {})
    } // Creates an instance of BookDetailScreen, passes in test data and empty callback functions
    // (Similar to how the screen would be used in the real app)

    println("Content set, attempting to verify title")

    // Assert, verify the expected outcome
    composeTestRule
        .onNodeWithText("Test Book Title")
        .assertExists() // Looks for a UI element containing our expected text, fails the test if
    // it's not found

    println("Test completed successfully")
  }

  @Test // Tests that all book information is displayed correctly
  fun `verify all fields are displayed for complete book`() {
    composeTestRule.setContent {
      BookDetailScreen(book = completeBook, onBackClick = {}, onSaveClick = {})
    }

    // Verify all text elements exist
    composeTestRule.onNodeWithText(completeBook.title).assertExists()
    composeTestRule.onNodeWithText("By ${completeBook.author}").assertExists()
    composeTestRule.onNodeWithText("Published: ${completeBook.publishedDate}").assertExists()
    composeTestRule.onNodeWithText("Pages: ${completeBook.pageCount}").assertExists()
    composeTestRule.onNodeWithText(completeBook.description).assertExists()

    // Verify categories
    completeBook.categories.forEach { category ->
      composeTestRule.onNodeWithText("• $category").assertExists()
    }
  }

  @Test // Tests how the screen handles books with minimal information
  fun `verify minimal book displays correctly without optional fields`() {
    composeTestRule.setContent {
      BookDetailScreen(book = minimalBook, onBackClick = {}, onSaveClick = {})
    }

    // Required fields should be present
    composeTestRule.onNodeWithText(minimalBook.title).assertExists()
    composeTestRule.onNodeWithText("By ${minimalBook.author}").assertExists()

    // Optional fields should not be present
    composeTestRule.onNodeWithText("Published:").assertDoesNotExist()
    composeTestRule.onNodeWithText("Pages:").assertDoesNotExist()
    composeTestRule.onNodeWithText("Categories:").assertDoesNotExist()
  }

  @Test
  fun `verify navigation buttons exist and are enabled`() {
    composeTestRule.setContent {
      BookDetailScreen(book = completeBook, onBackClick = {}, onSaveClick = {})
    }

    // Verify buttons exist and are enabled
    composeTestRule.onNodeWithText("Back to Search").assertExists().assertIsEnabled()

    composeTestRule.onNodeWithText("Save Book").assertExists().assertIsEnabled()
  }

  @Test
  fun `verify back button triggers callback`() {
    println("Starting test: verify back button triggers callback")
    // Arrange
    var backClicked = false // Sets up a flag (backClicked) to track if the callback was called

    composeTestRule.setContent {
      BookDetailScreen(book = completeBook, onBackClick = { backClicked = true }, onSaveClick = {})
    } // Provides callback that will set the flag to true and creates the screen with this callback

    // Act
    composeTestRule
        .onNodeWithText("Back to Search")
        .performClick() // Finds the "Back to Search" button using its text,
    // simulates clicking it using performClick()

    // Assert
    assert(backClicked) {
      "Back button click was not registered"
    } // Checks if our flag was changed to true, if true the callback was successfully called
    // if false, something went wrong with the button or callback

    println("Test completed successfully")
  }

  @Test // Ensures the correct book data is passed back
  fun `verify save button triggers callback with correct book`() {
    var savedBook: Book? = null

    composeTestRule.setContent {
      BookDetailScreen(
          book = completeBook, onBackClick = {}, onSaveClick = { book -> savedBook = book })
    }

    composeTestRule.onNodeWithText("Save Book").performClick()
    assert(savedBook == completeBook) { "Save button did not pass correct book to callback" }
  }

  @Test
  fun `verify book ID is displayed correctly`() {
    composeTestRule.setContent {
      BookDetailScreen(book = completeBook, onBackClick = {}, onSaveClick = {})
    }

    composeTestRule.onNodeWithText("Book ID: ${completeBook.id}").assertExists()
  }

  @Test // Tests UI behavior with large content
  fun `verify long description text is scrollable`() {
    val bookWithLongDescription =
        completeBook.copy(
            description = "A".repeat(1000) // Creates a string with 1000 'A' characters
            )

    composeTestRule.setContent {
      BookDetailScreen(book = bookWithLongDescription, onBackClick = {}, onSaveClick = {})
    }

    // Verify the description exists and can be found after scrolling
    composeTestRule
        .onNodeWithText(bookWithLongDescription.description)
        .assertExists()
        .performScrollTo()
        .assertIsDisplayed()
  }
}
