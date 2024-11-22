// RegisterScreen.kt
/**
 * Composable screen for user registration. Handles user input validation, password confirmation,
 * and registration state.
 */
package com.example.bookapp.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onRegisterSuccess: () -> Unit,
    onNavigateToLogin: () -> Unit
) {
  // State management for form fields
  var email by remember { mutableStateOf("") }
  var password by remember { mutableStateOf("") }
  var confirmPassword by remember { mutableStateOf("") }

  val state = viewModel.state.collectAsState().value

  DisposableEffect(Unit) { onDispose { viewModel.clearError() } }

  Column(
      modifier = Modifier.fillMaxSize().padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
        Text(text = "Create Account", style = MaterialTheme.typography.headlineMedium)

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

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth())

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = {
              if (password == confirmPassword) {
                viewModel.register(email, password)
              } else {
                viewModel.updateErrorState("Passwords do not match")
              }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled =
                !state.isLoading &&
                    email.isNotEmpty() &&
                    password.isNotEmpty() &&
                    confirmPassword.isNotEmpty()) {
              Text(if (state.isLoading) "Creating Account..." else "Register")
            }

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = onNavigateToLogin) { Text("Already have an account? Log in") }
      }

  LaunchedEffect(state.registrationSuccessful) {
    if (state.registrationSuccessful) {
      onRegisterSuccess()
    }
  }
}
