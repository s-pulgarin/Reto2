package com.example.app_reto2.data.repository

import com.example.app_reto2.util.Resource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

class UserRepositoryImpl(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : UserRepository {

    override suspend fun updateKycDocumentUrl(url: String): Resource<Unit> {
        val uid = firebaseAuth.currentUser?.uid
            ?: return Resource.Error("No hay una sesión activa.")

        return try {
            withTimeout(SAVE_TIMEOUT_MS) {
                firestore.collection(USERS_COLLECTION)
                    .document(uid)
                    .update("kycDocumentUrl", url)
                    .await()
            }
            Resource.Success(Unit)
        } catch (e: TimeoutCancellationException) {
            Resource.Error("No se pudo confirmar el guardado. Revisa tu conexión.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: FirebaseFirestoreException) {
            Resource.Error(
                when (e.code) {
                    FirebaseFirestoreException.Code.PERMISSION_DENIED -> "Sin permisos para actualizar el perfil."
                    FirebaseFirestoreException.Code.UNAVAILABLE -> "Sin conexión. Revisa tu internet."
                    else -> "Error al guardar el documento en tu perfil."
                },
                e
            )
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Ocurrió un error inesperado.", e)
        }
    }

    override fun observeUserBalance(uid: String): Flow<Double> = callbackFlow {
        val registration = firestore.collection(USERS_COLLECTION)
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                snapshot?.getDouble("balance")?.let { trySend(it) }
            }
        awaitClose { registration.remove() }
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val SAVE_TIMEOUT_MS = 10_000L
    }
}