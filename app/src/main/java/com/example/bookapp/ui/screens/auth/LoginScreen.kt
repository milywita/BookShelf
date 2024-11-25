// LoginScreen.kt
/**
 * Composable screen handling user login. Provides email/password input and validation with error
 * handling.
 */
package com.example.bookapp.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        TestUserSection(viewModel)
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

    LaunchedEffect(state.isLoggedIn) {
        if (state.isLoggedIn) onLoginSuccess()
    }
}

@Composable
private fun LoginHeader() {
    Text(
        text = "Welcome Back",
        style = MaterialTheme.typography.headlineMedium
    )
}

// For faster TESTING
@Composable
private fun TestUserSection(viewModel: AuthViewModel) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        TestUserButton(
            email = "test@test.com",
            label = "Test User 1",
            viewModel = viewModel
        )
        TestUserButton(
            email = "test2@test.com",
            label = "Test User 2",
            viewModel = viewModel
        )
    }
}

@Composable
private fun TestUserButton(
    email: String,
    label: String,
    viewModel: AuthViewModel
) {
    Button(
        onClick = { viewModel.signIn(email, "test123") },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondary
        )
    ) {
        Text(label)
    }
}

@Composable
private fun RegistrationSuccessMessage(isVisible: Boolean) {
    if (isVisible) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Registration successful! Please sign in.",
            color = MaterialTheme.colorScheme.primary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun ErrorMessage(error: String?) {
    if (error != null) {
        Spacer(modifier = Modifier.height(8.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(8.dp),
                style = MaterialTheme.typography.bodyMedium
            )
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
    Spacer(modifier = Modifier.height(32.dp))

    OutlinedTextField(
        value = credentials.email,
        onValueChange = onEmailChange,
        label = { Text("Email") },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(16.dp))

    OutlinedTextField(
        value = credentials.password,
        onValueChange = onPasswordChange,
        label = { Text("Password") },
        visualTransformation = PasswordVisualTransformation(),
        modifier = Modifier.fillMaxWidth(),
        singleLine = true
    )

    Spacer(modifier = Modifier.height(32.dp))

    Button(
        onClick = onSubmit,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isLoading && enabled
    ) {
        Text(if (isLoading) "Signing in..." else "Sign In")
    }
}

@Composable
private fun RegisterButton(onClick: () -> Unit) {
    Spacer(modifier = Modifier.height(16.dp))
    TextButton(onClick = onClick) {
        Text("Don't have an account? Register")
    }
}