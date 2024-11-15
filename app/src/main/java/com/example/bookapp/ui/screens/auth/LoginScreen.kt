// LoginScreen.kt
package com.example.bookapp.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    viewModel: AuthViewModel, // Takes AuthViewModel and success callback as parameters
    onLoginSuccess: () -> Unit
) {
  // Uses remember { mutableStateOf() } for email/password fields to preserve state across
  // recompositions
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }

  // Collects UI state from ViewModel using collectAsState()
  val state = viewModel.state.collectAsState().value

  // Main layout container
  Column(
      modifier = Modifier.fillMaxSize().padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
        Text(text = "Welcome Back", style = MaterialTheme.typography.headlineMedium)

        Spacer(modifier = Modifier.height(32.dp))

        // Text fields for email and password
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(32.dp))

        // Login button that calls viewModel.signIn()
        Button(
            onClick = { viewModel.signIn(email, password) },
            modifier = Modifier.fillMaxWidth(),
            enabled =
                !state.isLoading && email.isNotEmpty() && password.isNotEmpty() // Add validation
            ) {
              Text(if (state.isLoading) "Signing in..." else "Sign In")
            }
      }

  LaunchedEffect(state.isLoggedIn) {
    if (state.isLoggedIn) {
      onLoginSuccess()
    }
  }
}
