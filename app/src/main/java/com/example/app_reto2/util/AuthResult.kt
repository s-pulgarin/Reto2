package com.example.app_reto2.util

sealed class AuthResult<out T> {
    data class Success<out T>(val data: T) : AuthResult<T>()
    data class Error(
        val message: String,
        val exception: Throwable? = null
    ) : AuthResult<Nothing>()
}