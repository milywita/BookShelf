package com.example.bookapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import com.example.bookapp.ui.screens.BookSearchScreen
import com.example.bookapp.ui.theme.BookAppTheme

// Entry point of the app, contain basic app setup and navigation

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BookAppTheme {
                MainScreen()
            }
        }
    }
}

@Composable
fun MainScreen() {
    BookSearchScreen()
}