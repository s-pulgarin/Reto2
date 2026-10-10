package com.example.app_reto2.data.model

const val INITIAL_BALANCE = 10_000.0

object UserRole {
    const val USER = "user"
    const val ADMIN = "admin"
}

data class User(
    val uid: String = "",
    val email: String = "",
    val role: String = UserRole.USER,
    val balance: Double = INITIAL_BALANCE,
    val kycDocumentUrl: String? = null
)