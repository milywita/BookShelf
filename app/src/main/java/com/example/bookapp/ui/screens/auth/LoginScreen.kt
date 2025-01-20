// LoginScreen.kt
/**
 * Composable screen handling user login. Provides email/password input and validation with error
 * handling.
 */
package com.example.bookapp.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

data class LoginCredentials(val email: String = "", val password: String = "")

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit,
    onNavigateToRegister: () -> Unit
) {
  var credentials by remember { mutableStateOf(LoginCredentials()) }
  val state by viewModel.state.collectAsState()

  DisposableEffect(Unit) { onDispose { viewModel.clearError() } }

  Column(
      modifier = Modifier.fillMaxSize().padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
        LoginHeader()
        RegistrationSuccessMessage(state.registrationSuccessful)
        ErrorMessage(state.error)
        LoginForm(
            credentials = credentials,
            onEmailChange = { credentials = credentials.copy(email = it.trim()) },
            onPasswordChange = { credentials = credentials.copy(password = it) },
            onSubmit = { viewModel.signIn(credentials.email, credentials.password) },
            isLoading = state.isLoading,
            enabled = credentials.email.isNotEmpty() && credentials.password.isNotEmpty())
        RegisterButton(onNavigateToRegister)
      }

  LaunchedEffect(state.isLoggedIn) { if (state.isLoggedIn) onLoginSuccess() }
}

@Composable
private fun LoginHeader() {
  Text(text = "Welcome Back", style = MaterialTheme.typography.headlineMedium)
}

@Composable
private fun RegistrationSuccessMessage(isVisible: Boolean) {
  if (isVisible) {
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Registration successful! Please sign in.",
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.bodyMedium)
  }
}

@Composable
private fun ErrorMessage(error: String?) {
  if (error != null) {
    Spacer(modifier = Modifier.height(8.dp))
    Card(
        colors =
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
          Text(
              text = error,
              color = MaterialTheme.colorScheme.error,
              modifier = Modifier.padding(8.dp),
              style = MaterialTheme.typography.bodyMedium)
        }
  }
}

@Composable
private fun LoginForm(
    credentials: LoginCredentials,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onSubmit: () -> Unit,
    isLoading: Boolean,
    enabled: Boolean
) {
    var emailError by remember { mutableStateOf<String?>(null) }
    var isEmailFocused by remember { mutableStateOf(false) }
    val emailRegex = android.util.Patterns.EMAIL_ADDRESS

    fun validateEmail(email: String): String? {
        return when {
            email.isBlank() -> "Email cannot be empty"
            !emailRegex.matcher(email).matches() -> "Invalid email format"
            else -> null
        }
    }

    Spacer(modifier = Modifier.height(32.dp))

    // Email Field
    OutlinedTextField(
        value = credentials.email,
        onValueChange = {
            onEmailChange(it)
            // Clear the error while typing
            if (isEmailFocused) emailError = null
        },
        label = { Text("Email") },
        isError = emailError != null, // Show red outline if there is an error
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                if (!focusState.isFocused && credentials.email.isNotBlank()) {
                    // Validate only after the user moves focus away from the field
                    emailError = validateEmail(credentials.email)
                }
                isEmailFocused = focusState.isFocused
            },
        singleLine = true
    )
    if (emailError != null) {
        Text(
            text = emailError!!,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(start = 8.dp)
        )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Password Field
    OutlinedTextField(
        value = credentials.password,
        onValueChange = onPasswordChange,
        label = { Text("Password") },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(32.dp))

    // Submit Button
    Button(
        onClick = onSubmit,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading && enabled && emailError == null
    ) {
        Text(if (isLoading) "Signing in..." else "Sign In")
    }
}


@Composable
private fun RegisterButton(onClick: () -> Unit) {
  Spacer(modifier = Modifier.height(16.dp))
  TextButton(onClick = onClick) { Text("Don't have an account? Register") }
}
