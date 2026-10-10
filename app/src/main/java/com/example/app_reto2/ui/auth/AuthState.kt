package com.example.app_reto2.ui.auth

import com.example.app_reto2.data.model.User

sealed interface AuthState {
    data object Idle : AuthState
    data object Loading : AuthState
    data class Success(val user: User) : AuthState
    data class Error(val message: String) : AuthState
    data object LoggedOut : AuthState
}