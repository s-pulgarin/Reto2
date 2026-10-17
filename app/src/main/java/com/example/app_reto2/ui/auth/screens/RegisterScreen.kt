package com.example.app_reto2.ui.auth.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.app_reto2.R
import com.example.app_reto2.data.model.User
import com.example.app_reto2.ui.auth.AuthState
import com.example.app_reto2.ui.auth.AuthViewModel
import com.example.app_reto2.ui.auth.components.AuthFeedback
import com.example.app_reto2.ui.auth.components.EmailTextField
import com.example.app_reto2.ui.auth.components.PasswordTextField

@Composable
fun RegisterScreen(
    viewModel: AuthViewModel,
    onRegisterSuccess: (User) -> Unit,
    onNavigateToLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        val current = state
        if (current is AuthState.Success) {
            onRegisterSuccess(current.user)
            viewModel.resetState()
        }
    }

    RegisterContent(
        state = state,
        onRegisterClick = viewModel::register,
        onLoginClick = {
            viewModel.resetState()
            onNavigateToLogin()
        },
        modifier = modifier
    )
}

@Composable
private fun RegisterContent(
    state: AuthState,
    onRegisterClick: (email: String, password: String) -> Unit,
    onLoginClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val isLoading = state is AuthState.Loading
    val errorMessage = (state as? AuthState.Error)?.message

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = "Crea tu cuenta",
            style = MaterialTheme.typography.bodyLarge
        )

        Spacer(Modifier.height(32.dp))

        EmailTextField(
            value = email,
            onValueChange = { email = it },
            enabled = !isLoading
        )
        Spacer(Modifier.height(16.dp))
        PasswordTextField(
            value = password,
            onValueChange = { password = it },
            enabled = !isLoading,
            onDone = { onRegisterClick(email, password) }
        )

        Spacer(Modifier.height(16.dp))
        AuthFeedback(isLoading = isLoading, errorMessage = errorMessage)
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { onRegisterClick(email, password) },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Registrarse")
        }
        TextButton(onClick = onLoginClick, enabled = !isLoading) {
            Text("Ya tengo cuenta, iniciar sesión")
        }
    }
}