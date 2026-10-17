package com.example.app_reto2.data.repository

import com.example.app_reto2.data.model.User
import com.example.app_reto2.util.Resource

interface AuthRepository {
    val hasSession: Boolean

    suspend fun restoreSession(): Resource<User>

    suspend fun login(email: String, password: String): Resource<User>

    suspend fun register(email: String, password: String): Resource<User>

    suspend fun logout(): Resource<Unit>
}