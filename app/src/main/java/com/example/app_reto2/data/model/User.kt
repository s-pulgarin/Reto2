package com.example.app_reto2.data.model

// Debe coincidir con la regla de creación de users/{uid} en Firestore.
const val INITIAL_BALANCE = 300_000.0

object UserRole {
    const val USER = "user"
    const val ADMIN = "admin"
}

data class User(
    val uid: String = "",
    val email: String = "",
    val role: String = UserRole.USER,
    val balance: Double = INITIAL_BALANCE,
    val kycDocumentUrl: String? = null,
    /** Unidades que posee el usuario por par operado (clave = `Transaction.currencyPair`, ej. "BTCUSDT"). */
    val portfolio: Map<String, Double> = emptyMap()
)