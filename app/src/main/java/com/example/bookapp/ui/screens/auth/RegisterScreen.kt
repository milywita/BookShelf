// RegisterScreen.kt
package com.example.bookapp.ui.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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

    // Separate state for showing errors
    var emailError by remember { mutableStateOf<String?>(null) }
    var passwordError by remember { mutableStateOf<String?>(null) }
    var confirmPasswordError by remember { mutableStateOf<String?>(null) }

    // Track whether fields have been interacted with
    var emailTouched by remember { mutableStateOf(false) }
    var passwordTouched by remember { mutableStateOf(false) }
    var confirmPasswordTouched by remember { mutableStateOf(false) }

    val state = viewModel.state.collectAsState().value

    // Email validation
    fun validateEmail(email: String): String? {
        // Trim the email to remove any leading/trailing whitespace
        val trimmedEmail = email.trim()

        return when {
            trimmedEmail.isBlank() -> "Email cannot be empty"
            // More lenient email regex that allows most common email formats
            !Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\$").matches(trimmedEmail) -> "Invalid email format"
            else -> null
        }
    }

    // Password validation
    fun validatePassword(password: String): String? {
        return when {
            password.isBlank() -> "Password cannot be empty"
            password.length < 6 -> "Password must be at least 6 characters"
            else -> null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Create Account", style = MaterialTheme.typography.headlineMedium)

        if (state.error != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state.error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Email Field
        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                // Reset email error when user starts typing
                if (emailTouched) emailError = null
            },
            label = { Text("Email") },
            isError = emailTouched && emailError != null,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused && email.isNotBlank()) {
                        emailTouched = true
                        emailError = validateEmail(email)
                    }
                }
        )
        if (emailTouched && emailError != null) {
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
            value = password,
            onValueChange = {
                password = it
                // Reset password error when user starts typing
                if (passwordTouched) passwordError = null
            },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            isError = passwordTouched && passwordError != null,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused && password.isNotBlank()) {
                        passwordTouched = true
                        passwordError = validatePassword(password)
                    }
                }
        )
        if (passwordTouched && passwordError != null) {
            Text(
                text = passwordError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Confirm Password Field
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                // Reset confirm password error when user starts typing
                if (confirmPasswordTouched) confirmPasswordError = null
            },
            label = { Text("Confirm Password") },
            visualTransformation = PasswordVisualTransformation(),
            isError = confirmPasswordTouched && confirmPasswordError != null,
            modifier = Modifier
                .fillMaxWidth()
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused && confirmPassword.isNotBlank()) {
                        confirmPasswordTouched = true
                        confirmPasswordError = when {
                            confirmPassword.isBlank() -> "Confirm password cannot be empty"
                            confirmPassword != password -> "Passwords do not match"
                            else -> null
                        }
                    }
                }
        )
        if (confirmPasswordTouched && confirmPasswordError != null) {
            Text(
                text = confirmPasswordError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Register Button
        Button(
            onClick = {
                // Only validate and show errors when trying to submit
                emailError = validateEmail(email)
                passwordError = validatePassword(password)
                confirmPasswordError = when {
                    confirmPassword.isBlank() -> "Confirm password cannot be empty"
                    confirmPassword != password -> "Passwords do not match"
                    else -> null
                }

                // Mark all fields as touched
                emailTouched = true
                passwordTouched = true
                confirmPasswordTouched = true

                // Proceed with registration only if all validations pass
                if (emailError == null && passwordError == null && confirmPasswordError == null) {
                    viewModel.register(email, password)
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading
        ) {
            Text(if (state.isLoading) "Creating Account..." else "Register")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Navigate to Login Button
        TextButton(onClick = onNavigateToLogin) {
            Text("Already have an account? Log in")
        }
    }

    // Navigate on successful registration
    LaunchedEffect(state.registrationSuccessful) {
        if (state.registrationSuccessful) {
            onRegisterSuccess()
        }
    }
}