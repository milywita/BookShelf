// LoginScreen.kt
/**
 * Composable screen handling user login. Provides email/password input and validation with error
 * handling.
 */
package com.example.bookapp.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  val state = viewModel.state.collectAsState().value

  DisposableEffect(Unit) { onDispose { viewModel.clearError() } }

  Column(
      modifier = Modifier.fillMaxSize().padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
        Text(text = "Welcome Back", style = MaterialTheme.typography.headlineMedium)

        if (state.registrationSuccessful) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
              text = "Registration successful! Please sign in.",
              color = MaterialTheme.colorScheme.primary,
              style = MaterialTheme.typography.bodyMedium)
        }

        if (state.error != null) {
          Spacer(modifier = Modifier.height(8.dp))
          Card(
              colors =
                  CardDefaults.cardColors(
                      containerColor = MaterialTheme.colorScheme.errorContainer,
                  )) {
                Text(
                    text = state.error,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(8.dp),
                    style = MaterialTheme.typography.bodyMedium)
              }
        }

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = email,
            onValueChange = { email = it.trim() },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true)

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true)

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { viewModel.signIn(email.trim(), password) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading && email.trim().isNotEmpty() && password.isNotEmpty()) {
              Text(if (state.isLoading) "Signing in..." else "Sign In")
            }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToRegister) { Text("Don't have an account? Register") }
      }

  LaunchedEffect(state.isLoggedIn) {
    if (state.isLoggedIn) {
      onLoginSuccess()
    }
  }
}
