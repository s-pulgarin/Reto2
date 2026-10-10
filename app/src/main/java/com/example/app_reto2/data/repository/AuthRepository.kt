package com.example.app_reto2.data.repository

import com.example.app_reto2.data.model.User
import com.example.app_reto2.util.AuthResult

interface AuthRepository {
    val hasSession: Boolean

    suspend fun restoreSession(): AuthResult<User>

    suspend fun login(email: String, password: String): AuthResult<User>

    suspend fun register(email: String, password: String): AuthResult<User>

    suspend fun logout(): AuthResult<Unit>
}