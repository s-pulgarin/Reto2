package com.example.app_reto2.data.repository

import com.example.app_reto2.data.model.Transaction
import com.example.app_reto2.data.model.TransactionStatus
import com.example.app_reto2.data.model.TransactionType
import com.example.app_reto2.util.Resource
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import com.google.firebase.firestore.Transaction as FirestoreTransaction

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
        } catch (e: Exception) {
            Resource.Error(e.toFriendlyMessage(), e)
        }

    override fun getAllTransactions(): Flow<Resource<List<Transaction>>> = callbackFlow {
        val registration = firestore.collection(TRANSACTIONS_COLLECTION)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.toFriendlyMessage(), error))
                    return@addSnapshotListener
                }
                val transactions = snapshot?.documents.orEmpty().mapNotNull { document ->
                    document.toObject(Transaction::class.java)?.copy(transactionId = document.id)
                }
                trySend(Resource.Success(transactions))
            }
        awaitClose { registration.remove() }
    }

    override fun getUserTransactions(uid: String): Flow<Resource<List<Transaction>>> = callbackFlow {
        val registration = firestore.collection(TRANSACTIONS_COLLECTION)
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(Resource.Error(error.toFriendlyMessage(), error))
                    return@addSnapshotListener
                }
                val transactions = snapshot?.documents.orEmpty()
                    .mapNotNull { document ->
                        document
                            .toObject(Transaction::class.java, ServerTimestampBehavior.ESTIMATE)
                            ?.copy(transactionId = document.id)
                    }
                    .sortedByDescending { it.timestamp?.time ?: Long.MAX_VALUE }
                trySend(Resource.Success(transactions))
            }
        awaitClose { registration.remove() }
    }

    override suspend fun auditTransaction(
        transaction: Transaction,
        isApproved: Boolean
    ): Resource<Unit> =
        try {
            val transactionRef = firestore.collection(TRANSACTIONS_COLLECTION)
                .document(transaction.transactionId)

            firestore.runTransaction { firestoreTransaction ->
                val snapshot = firestoreTransaction.get(transactionRef)
                check(snapshot.getString("status") == TransactionStatus.PENDING) {
                    "La transacción ya fue auditada."
                }
                val newStatus = if (isApproved) {
                    applyBalanceChange(firestoreTransaction, snapshot)
                } else {
                    TransactionStatus.REJECTED
                }
                firestoreTransaction.update(transactionRef, "status", newStatus)
                Unit
            }.await()

            Resource.Success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Resource.Error(e.toFriendlyMessage(), e)
        }

    /**
     * BUY resta `amount × executionPrice` del balance y suma `amount` al portfolio del par;
     * SELL hace lo inverso. Si una compra dejaría el balance en negativo, o una venta supera
     * las unidades en el portfolio, la transacción se rechaza sin tocar el perfil.
     * Devuelve el estado final a asignar a la transacción.
     */
    private fun applyBalanceChange(
        firestoreTransaction: FirestoreTransaction,
        transactionSnapshot: DocumentSnapshot
    ): String {
        val userId = transactionSnapshot.getString("userId")
            ?: error("La transacción no tiene usuario.")
        val amount = transactionSnapshot.getDouble("amount")
            ?: error("La transacción no tiene cantidad.")
        val price = transactionSnapshot.getDouble("executionPrice")
            ?: error("La transacción no tiene precio de ejecución.")
        val currencyPair = transactionSnapshot.getString("currencyPair")
            ?.takeIf { it.isNotBlank() }
            ?: error("La transacción no tiene par de divisas.")
        val type = transactionSnapshot.getString("type")

        val userRef = firestore.collection(USERS_COLLECTION).document(userId)
        val userSnapshot = firestoreTransaction.get(userRef)
        val balance = userSnapshot.getDouble("balance")
            ?: error("No se encontró el balance del usuario.")
        val portfolioPath = FieldPath.of(PORTFOLIO_FIELD, currencyPair)
        val holdings = (userSnapshot.get(portfolioPath) as? Number)?.toDouble() ?: 0.0

        val cost = amount * price
        val (newBalance, newHoldings) = if (type == TransactionType.BUY) {
            balance - cost to holdings + amount
        } else {
            balance + cost to holdings - amount
        }
        if (newBalance < 0.0 || newHoldings < -QUANTITY_EPSILON) return TransactionStatus.REJECTED

        firestoreTransaction.update(
            userRef,
            FieldPath.of("balance"), newBalance,
            portfolioPath, newHoldings.coerceAtLeast(0.0)
        )
        return TransactionStatus.APPROVED
    }

    private fun Throwable.toFriendlyMessage(): String = when {
        this is FirebaseFirestoreException -> when (code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> "Sin permisos para esta operación."
            FirebaseFirestoreException.Code.UNAVAILABLE -> "Sin conexión. Revisa tu internet."
            else -> "Error al acceder a los datos."
        }
        else -> message ?: "Ocurrió un error inesperado."
    }

    private companion object {
        const val TRANSACTIONS_COLLECTION = "transactions"
        const val USERS_COLLECTION = "users"
        const val PORTFOLIO_FIELD = "portfolio"
        const val SAVE_TIMEOUT_MS = 10_000L

        /** Tolerancia para errores de redondeo de Double al restar unidades. */
        const val QUANTITY_EPSILON = 1e-9
    }
}