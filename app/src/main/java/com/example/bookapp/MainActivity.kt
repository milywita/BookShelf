// MainActivity.kt
package com.example.bookapp

import com.example.bookapp.data.repository.AuthRepository
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.bookapp.ui.screens.BookSearchScreen
import com.example.bookapp.ui.screens.auth.AuthViewModel
import com.example.bookapp.ui.screens.auth.AuthViewModelFactory
import com.example.bookapp.ui.screens.auth.LoginScreen
import com.example.bookapp.ui.theme.BookAppTheme


// MainActivity.kt
class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(AuthRepository())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BookAppTheme {
                val navController = rememberNavController()
                val isLoggedIn by authViewModel.state.collectAsState()

                NavHost(
                    navController = navController,
                    startDestination = if (isLoggedIn.isLoggedIn) "search" else "login"
                ) {
                    composable("login") {
                        LoginScreen(
                            viewModel = authViewModel,
                            onLoginSuccess = {
                                navController.navigate("search") {
                                    popUpTo("login") { inclusive = true }
                                }
                            }
                        )
                    }
                    composable("search") {
                        if (!isLoggedIn.isLoggedIn) {
                            LaunchedEffect(Unit) {
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                        BookSearchScreen(
                            onSignOut = {
                                authViewModel.signOut()
                                navController.navigate("login") {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}