package com.example.app_reto2.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.app_reto2.data.model.AuthUser
import com.example.app_reto2.data.repository.AuthRepository
import com.example.app_reto2.util.AuthResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    fun login(email: String, password: String) {
        if (!canSubmit(email, password)) return
        _state.value = AuthState.Loading
        viewModelScope.launch {
            _state.value = authRepository.login(email.trim(), password).toState()
        }
    }

    fun register(email: String, password: String) {
        if (!canSubmit(email, password)) return
        _state.value = AuthState.Loading
        viewModelScope.launch {
            _state.value = authRepository.register(email.trim(), password).toState()
        }
    }

    fun resetState() {
        _state.value = AuthState.Idle
    }

    private fun canSubmit(email: String, password: String): Boolean {
        if (_state.value is AuthState.Loading) return false
        if (email.isBlank() || password.isBlank()) {
            _state.value = AuthState.Error("Completa el correo y la contraseña.")
            return false
        }
        return true
    }

    private fun AuthResult<AuthUser>.toState(): AuthState = when (this) {
        is AuthResult.Success -> AuthState.Success(data)
        is AuthResult.Error -> AuthState.Error(message)
    }
}