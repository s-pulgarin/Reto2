package com.example.app_reto2.data.repository

import com.example.app_reto2.data.model.Transaction
import com.example.app_reto2.util.Resource
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {

    suspend fun saveTransaction(transaction: Transaction): Resource<Unit>

    /** Todas las transacciones en tiempo real (vista de admin), de la más reciente a la más antigua. */
    fun getAllTransactions(): Flow<Resource<List<Transaction>>>

    /** Transacciones del usuario [uid] en tiempo real, de la más reciente a la más antigua. */
    fun getUserTransactions(uid: String): Flow<Resource<List<Transaction>>>

    /** Aprueba (ajustando balance y portfolio del usuario) o rechaza una transacción PENDING. */
    suspend fun auditTransaction(transaction: Transaction, isApproved: Boolean): Resource<Unit>
}