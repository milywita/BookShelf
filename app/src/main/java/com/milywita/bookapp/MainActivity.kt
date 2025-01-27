// MainActivity.kt

package com.milywita.bookapp

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
import com.milywita.bookapp.data.repository.AuthRepository
import com.milywita.bookapp.ui.screens.BookSearchScreen
import com.milywita.bookapp.ui.screens.auth.AuthViewModel
import com.milywita.bookapp.ui.screens.auth.AuthViewModelFactory
import com.milywita.bookapp.ui.screens.auth.LoginScreen
import com.milywita.bookapp.ui.screens.auth.RegisterScreen
import com.milywita.bookapp.ui.theme.BookAppTheme

class MainActivity : ComponentActivity() {
  private val authViewModel: AuthViewModel by viewModels { AuthViewModelFactory(AuthRepository()) }
  private val authRepository = AuthRepository()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      BookAppTheme {
        val navController = rememberNavController()
        val isLoggedIn by authViewModel.state.collectAsState()
        NavHost(
            navController = navController,
            startDestination = if (isLoggedIn.isLoggedIn) "search" else "login") {
              composable("login") {
                LoginScreen(
                    viewModel = authViewModel,
                    onLoginSuccess = {
                      navController.navigate("search") { popUpTo("login") { inclusive = true } }
                    },
                    onNavigateToRegister = { navController.navigate("register") })
              }
              composable("register") {
                RegisterScreen(
                    viewModel = authViewModel,
                    onRegisterSuccess = {
                      navController.navigate("login") { popUpTo("login") { inclusive = true } }
                    },
                    onNavigateToLogin = { navController.popBackStack() })
              }
              composable("search") {
                if (!isLoggedIn.isLoggedIn) {
                  LaunchedEffect(Unit) {
                    // Redirect to login if authentication is missing
                    navController.navigate("login") { popUpTo(0) { inclusive = true } }
                  }
                }
                BookSearchScreen(
                    onSignOut = {
                      authViewModel.signOut()
                      navController.navigate("login") { popUpTo(0) { inclusive = true } }
                    })
              }
            }
      }
    }
  }
}
