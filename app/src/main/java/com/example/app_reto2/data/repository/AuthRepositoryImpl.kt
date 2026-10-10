package com.example.app_reto2.data.repository

package com.example.app_reto2.data.repository

import com.example.app_reto2.data.model.AuthUser
import com.example.app_reto2.util.AuthResult
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
) : AuthRepository {

    override val currentUser: AuthUser?
        get() = firebaseAuth.currentUser?.toAuthUser()

    override suspend fun login(email: String, password: String): AuthResult<AuthUser> =
        try {
            val user = firebaseAuth.signInWithEmailAndPassword(email, password)
                .await()
                .user
                ?: return AuthResult.Error("No se pudo obtener el usuario.")
            AuthResult.Success(user.toAuthUser())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AuthResult.Error(e.toFriendlyMessage(), e)
        }

    override suspend fun register(
        email: String,
        password: String,
        displayName: String?
    ): AuthResult<AuthUser> =
        try {
            val user = firebaseAuth.createUserWithEmailAndPassword(email, password)
                .await()
                .user
                ?: return AuthResult.Error("No se pudo crear el usuario.")

            if (!displayName.isNullOrBlank()) {
                val profile = UserProfileChangeRequest.Builder()
                    .setDisplayName(displayName.trim())
                    .build()
                user.updateProfile(profile).await()
            }
            AuthResult.Success(user.toAuthUser())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AuthResult.Error(e.toFriendlyMessage(), e)
        }

    override suspend fun logout(): AuthResult<Unit> =
        try {
            firebaseAuth.signOut()
            AuthResult.Success(Unit)
        } catch (e: Exception) {
            AuthResult.Error(e.toFriendlyMessage(), e)
        }

    private fun FirebaseUser.toAuthUser() = AuthUser(
        id = uid,
        email = email,
        displayName = displayName
    )

    private fun Exception.toFriendlyMessage(): String = when (this) {
        is FirebaseAuthInvalidUserException -> "No existe una cuenta con ese correo."
        is FirebaseAuthInvalidCredentialsException -> "Correo o contraseña incorrectos."
        is FirebaseAuthUserCollisionException -> "Ya existe una cuenta con ese correo."
        is FirebaseAuthWeakPasswordException -> "La contraseña es demasiado débil."
        is FirebaseNetworkException -> "Sin conexión. Revisa tu internet."
        else -> message ?: "Ocurrió un error inesperado."
    }
}