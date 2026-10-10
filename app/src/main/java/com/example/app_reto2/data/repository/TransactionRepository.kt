package com.example.app_reto2.data.repository

import com.example.app_reto2.data.model.Transaction
import com.example.app_reto2.util.Resource

interface TransactionRepository {
    suspend fun saveTransaction(transaction: Transaction): Resource<Unit>
}