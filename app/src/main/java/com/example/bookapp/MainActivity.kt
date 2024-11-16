// MainActivity.kt
package com.example.bookapp

import com.example.bookapp.ui.screens.auth.RegisterScreen
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
import com.example.bookapp.data.repository.AuthRepository
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
                val navController = rememberNavController() // Creates navigation controller
                val isLoggedIn by authViewModel.state.collectAsState() // Collects login state from ViewModel

                // Sets up navigation system,
                // if `logged in` then `search screen`,
                // if `not logged in` then `login screen`
                NavHost(
                    navController = navController,
                    startDestination = if (isLoggedIn.isLoggedIn) "search" else "login"
                ) {
                    composable("login") {
                        LoginScreen(
                            viewModel = authViewModel,
                            onLoginSuccess = {
                                //  success callback = go to search
                                navController.navigate("search") {
                                    // removes login from back stack
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onNavigateToRegister = {
                                navController.navigate("register")
                            }
                        )
                    }
                    composable("register") {
                        RegisterScreen(
                            viewModel = authViewModel,
                            onRegisterSuccess = {
                                // Navigate back to login instead of search
                                navController.navigate("login") {
                                    // Remove register screen from back stack
                                    popUpTo("login") { inclusive = true }
                                }
                            },
                            onNavigateToLogin = {
                                navController.popBackStack()
                            }
                        )
                    }
                    composable("search") {
                        // Authentication check
                        // Checks if user is NOT logged in
                        if (!isLoggedIn.isLoggedIn) {
                            // Runs once when the screen is shown
                            LaunchedEffect(Unit) {
                                navController.navigate("login") {
                                    // If not logged in, forces navigation to login screen
                                    // popUpTo(0) means "remove ALL screens from back stack"
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        }
                        BookSearchScreen(
                            onSignOut = {
                                authViewModel.signOut() // First sign out from Firebase
                                navController.navigate("login") { // Then navigate
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