package com.example.app_reto2.data.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

object TransactionType {
    const val BUY = "BUY"
    const val SELL = "SELL"
}

object TransactionStatus {
    const val PENDING = "PENDING"
    const val APPROVED = "APPROVED"
    const val REJECTED = "REJECTED"
}

data class Transaction(
    val transactionId: String = "",
    val userId: String = "",
    val type: String = TransactionType.BUY,
    val currencyPair: String = "",
    val amount: Double = 0.0,
    val executionPrice: Double = 0.0,
    @ServerTimestamp val timestamp: Date? = null,
    val status: String = TransactionStatus.PENDING
)