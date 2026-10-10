package com.example.app_reto2.data.repository

import com.example.app_reto2.data.model.Transaction
import com.example.app_reto2.util.Resource
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class TransactionRepositoryImpl(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : TransactionRepository {

    override suspend fun saveTransaction(transaction: Transaction): Resource<Unit> =
        try {
            val document = firestore.collection(TRANSACTIONS_COLLECTION).document()
            withTimeout(SAVE_TIMEOUT_MS) {
                document.set(transaction.copy(transactionId = document.id)).await()
            }
            Resource.Success(Unit)
        } catch (e: TimeoutCancellationException) {
            Resource.Error("No se pudo confirmar la operación. Revisa tu conexión.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseFirestoreException) {
            Resource.Error(
                when (e.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> "Sin permisos para registrar la operación."
                    FirebaseFirestoreException.Code.UNAVAILABLE -> "Sin conexión. Revisa tu internet."
                    else -> "Error al guardar la operación."
                },
                e
            )
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Ocurrió un error inesperado.", e)
        }

    private companion object {
        const val TRANSACTIONS_COLLECTION = "transactions"
        const val SAVE_TIMEOUT_MS = 10_000L
    }
}