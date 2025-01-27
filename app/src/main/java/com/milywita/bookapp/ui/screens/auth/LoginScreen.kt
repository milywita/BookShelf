// LoginScreen.kt
/**
 * Composable screen handling user login. Provides email/password input and validation with error
 * handling.
 */
package com.milywita.bookapp.ui.screens.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
  var showForgotPasswordDialog by remember { mutableStateOf(false) }
  var loginAttempted by remember { mutableStateOf(false) }

  DisposableEffect(Unit) { onDispose { viewModel.clearError() } }

  Column(
      modifier =
          Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center) {
        LoginHeader()

        // Show registration success message
        RegistrationSuccessMessage(state.registrationSuccessful)

        // Show error message
        ErrorMessage(state.error)

        // Main login form
        LoginForm(
            credentials = credentials,
            onEmailChange = { credentials = credentials.copy(email = it.trim()) },
            onPasswordChange = { credentials = credentials.copy(password = it) },
            onSubmit = {
              loginAttempted = true
              viewModel.signIn(credentials.email, credentials.password)
            },
            isLoading = state.isLoading,
            enabled = credentials.email.isNotEmpty() && credentials.password.isNotEmpty())

        // Forgot password button
        TextButton(
            onClick = { showForgotPasswordDialog = true },
            modifier = Modifier.padding(top = 8.dp)) {
              Text("Forgot Password?")
            }

        // Register button
        RegisterButton(onNavigateToRegister)
      }

  // Show forgot password dialog when needed
  if (showForgotPasswordDialog) {
    ForgotPasswordDialog(
        onDismiss = { showForgotPasswordDialog = false },
        onSubmit = { email -> viewModel.sendPasswordResetEmail(email) },
        isLoading = state.isLoading,
        resetEmailSent = state.passwordResetEmailSent)
  }

  LaunchedEffect(state.isLoggedIn) { if (state.isLoggedIn) onLoginSuccess() }
}

@Composable
private fun LoginHeader() {
  Text(
      text = "Welcome Back",
      style = MaterialTheme.typography.headlineMedium,
      color = MaterialTheme.colorScheme.onBackground)
}

@Composable
private fun RegistrationSuccessMessage(isVisible: Boolean) {
  if (isVisible) {
    Spacer(modifier = Modifier.height(8.dp))
    Text(
        text = "Registration successful! Please check your inbox.",
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
  var passwordError by remember { mutableStateOf<String?>(null) }
  var isEmailFocused by remember { mutableStateOf(false) }
  val emailRegex = android.util.Patterns.EMAIL_ADDRESS

  // Function to validate email
  fun validateEmail(email: String): String? {
    return when {
      email.isBlank() -> "Email cannot be empty"
      !emailRegex.matcher(email).matches() -> "Invalid email format"
      else -> null
    }
  }

  // Function to validate password
  fun validatePassword(password: String): String? {
    return when {
      password.isBlank() -> "Password cannot be empty"
      password.length < 6 -> "Password must be at least 6 characters"
      else -> null
    }
  }

  Spacer(modifier = Modifier.height(32.dp))

  // Email Field
  OutlinedTextField(
      value = credentials.email,
      onValueChange = {
        onEmailChange(it)
        if (isEmailFocused) emailError = null // Clear the error while typing
      },
      label = { Text("Email") },
      isError = emailError != null,
      modifier =
          Modifier.fillMaxWidth().onFocusChanged { focusState ->
            if (!focusState.isFocused && credentials.email.isNotBlank()) {
              emailError = validateEmail(credentials.email)
            }
            isEmailFocused = focusState.isFocused
          },
      singleLine = true)
  if (emailError != null) {
    Text(
        text = emailError!!,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(start = 8.dp))
  }

  Spacer(modifier = Modifier.height(16.dp))

  // Password Field
  OutlinedTextField(
      value = credentials.password,
      onValueChange = {
        onPasswordChange(it)
        passwordError = null // Clear error while typing
      },
      label = { Text("Password") },
      visualTransformation = PasswordVisualTransformation(),
      isError = passwordError != null,
      modifier = Modifier.fillMaxWidth(),
      singleLine = true)
  if (passwordError != null) {
    Text(
        text = passwordError!!,
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(start = 8.dp))
  }

  Spacer(modifier = Modifier.height(32.dp))

  // Submit Button
  Button(
      onClick = {
        emailError = validateEmail(credentials.email)
        passwordError = validatePassword(credentials.password)
        if (emailError == null && passwordError == null) {
          onSubmit()
        }
      },
      modifier = Modifier.fillMaxWidth(),
      enabled = !isLoading && enabled && emailError == null && passwordError == null) {
        Text(if (isLoading) "Signing in..." else "Sign In")
      }
}

@Composable
private fun RegisterButton(onClick: () -> Unit) {
  Spacer(modifier = Modifier.height(16.dp))
  TextButton(onClick = onClick) { Text("Don't have an account? Register") }
}

@Composable
private fun VerificationMessage(verificationEmailSent: Boolean, onResendVerification: () -> Unit) {
  Card(
      modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
      colors =
          CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(
            modifier =
                Modifier.padding(16.dp)
                    .fillMaxWidth(), // Ensure the content is centered horizontally
            horizontalAlignment = Alignment.CenterHorizontally, // Center the text and button
            verticalArrangement = Arrangement.spacedBy(8.dp) // Add spacing between elements
            ) {
              Text(
                  text = "Please verify your email to continue",
                  style = MaterialTheme.typography.bodyMedium,
                  color = MaterialTheme.colorScheme.onPrimaryContainer,
                  modifier = Modifier.fillMaxWidth(),
                  textAlign = TextAlign.Center // Center the text horizontally
                  )

              if (verificationEmailSent) {
                Text(
                    text = "Verification email sent! Please check your inbox.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center // Center the text horizontally
                    )
              } else {
                TextButton(
                    onClick = onResendVerification,
                    modifier = Modifier.align(Alignment.CenterHorizontally) // Center the button
                    ) {
                      Text("Resend verification email")
                    }
              }
            }
      }
}

@Composable
private fun ForgotPasswordDialog(
    onDismiss: () -> Unit,
    onSubmit: (String) -> Unit,
    isLoading: Boolean,
    resetEmailSent: Boolean
) {
  var email by remember { mutableStateOf("") }
  var emailError by remember { mutableStateOf<String?>(null) }

  AlertDialog(
      onDismissRequest = onDismiss,
      title = { Text(text = "Reset Password") },
      text = {
        Column {
          if (resetEmailSent) {
            Text(
                text = "Password reset email sent! Please check your inbox.",
                color = MaterialTheme.colorScheme.primary)
          } else {
            OutlinedTextField(
                value = email,
                onValueChange = {
                  email = it
                  emailError = null
                },
                label = { Text("Email") },
                isError = emailError != null,
                singleLine = true,
                modifier = Modifier.fillMaxWidth())

            if (emailError != null) {
              Text(
                  text = emailError!!,
                  color = MaterialTheme.colorScheme.error,
                  style = MaterialTheme.typography.bodySmall,
                  modifier = Modifier.padding(start = 8.dp, top = 4.dp))
            }
          }
        }
      },
      confirmButton = {
        Button(
            onClick = {
              if (android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                onSubmit(email)
              } else {
                emailError = "Please enter a valid email"
              }
            },
            enabled = !isLoading && !resetEmailSent) {
              Text(text = if (resetEmailSent) "Close" else "Send Reset Email")
            }
      },
      dismissButton = {
        if (!resetEmailSent) {
          TextButton(onClick = onDismiss) { Text("Cancel") }
        }
      })
}
