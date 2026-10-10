package com.example.app_reto2.data.repository

import com.example.app_reto2.data.model.AuthUser
import com.example.app_reto2.util.AuthResult

interface AuthRepository {
    val currentUser: AuthUser?

    suspend fun login(email: String, password: String): AuthResult<AuthUser>

    suspend fun register(
        email: String,
        password: String,
        displayName: String? = null
    ): AuthResult<AuthUser>

    suspend fun logout(): AuthResult<Unit>
}