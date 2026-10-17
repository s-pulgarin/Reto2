package com.example.app_reto2.data.repository

import com.example.app_reto2.data.model.INITIAL_BALANCE
import com.example.app_reto2.data.model.User
import com.example.app_reto2.data.model.UserRole
import com.example.app_reto2.util.Resource
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.tasks.await

class AuthRepositoryImpl(
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : AuthRepository {

    private val usersCollection get() = firestore.collection(USERS_COLLECTION)

    override val hasSession: Boolean
        get() = firebaseAuth.currentUser != null

    override suspend fun restoreSession(): Resource<User> =
        try {
            val firebaseUser = firebaseAuth.currentUser
                ?: return Resource.Error("No hay una sesión activa.")
            Resource.Success(fetchOrCreateProfile(firebaseUser))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Resource.Error(e.toFriendlyMessage(), e)
        }

    override suspend fun login(email: String, password: String): Resource<User> =
        try {
            val firebaseUser = firebaseAuth.signInWithEmailAndPassword(email, password)
                .await()
                .user
                ?: return Resource.Error("No se pudo obtener el usuario.")
            try {
                Resource.Success(fetchOrCreateProfile(firebaseUser))
            } catch (e: Exception) {
                firebaseAuth.signOut() // evita sesión a medias sin rol
                throw e
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Resource.Error(e.toFriendlyMessage(), e)
        }

    override suspend fun register(email: String, password: String): Resource<User> =
        try {
            val firebaseUser = firebaseAuth.createUserWithEmailAndPassword(email, password)
                .await()
                .user
                ?: return Resource.Error("No se pudo crear el usuario.")
            try {
                Resource.Success(createProfile(firebaseUser))
            } catch (e: Exception) {
                runCatching { firebaseUser.delete().await() } // rollback de la cuenta
                throw e
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Resource.Error(e.toFriendlyMessage(), e)
        }

    override suspend fun logout(): Resource<Unit> =
        try {
            firebaseAuth.signOut()
            Resource.Success(Unit)
        } catch (e: Exception) {
            Resource.Error(e.toFriendlyMessage(), e)
        }

    /** Lee users/{uid}; si no existe (cuentas anteriores a esta fase) lo crea con rol "user". */
    private suspend fun fetchOrCreateProfile(firebaseUser: FirebaseUser): User {
        val snapshot = usersCollection.document(firebaseUser.uid).get().await()
        return snapshot.toObject(User::class.java)?.copy(uid = firebaseUser.uid)
            ?: createProfile(firebaseUser)
    }

    private suspend fun createProfile(firebaseUser: FirebaseUser): User {
        val user = User(
            uid = firebaseUser.uid,
            email = firebaseUser.email.orEmpty(),
            role = UserRole.USER,
            balance = INITIAL_BALANCE
        )
        usersCollection.document(user.uid).set(user).await()
        return user
    }

    // FirebaseAuthWeakPasswordException hereda de FirebaseAuthInvalidCredentialsException,
    // por eso va primero; un correo mal formado también llega como InvalidCredentials.
    private fun Exception.toFriendlyMessage(): String = when (this) {
        is FirebaseAuthWeakPasswordException -> "La contraseña debe tener al menos 6 caracteres."
        is FirebaseAuthInvalidCredentialsException ->
            if (errorCode == ERROR_INVALID_EMAIL) "El formato del correo es inválido."
            else "Correo o contraseña incorrectos."
        is FirebaseAuthUserCollisionException -> "El correo ya está registrado."
        is FirebaseAuthInvalidUserException ->
            if (errorCode == ERROR_USER_DISABLED) "Esta cuenta está deshabilitada."
            else "No existe una cuenta con ese correo."
        is FirebaseTooManyRequestsException -> "Demasiados intentos. Espera un momento e inténtalo de nuevo."
        is FirebaseNetworkException -> "Sin conexión. Revisa tu internet."
        is FirebaseFirestoreException -> when (code) {
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> "Sin permisos para acceder al perfil."
            FirebaseFirestoreException.Code.UNAVAILABLE -> "Sin conexión. Revisa tu internet."
            else -> "Error al acceder al perfil."
        }
        else -> message ?: "Ocurrió un error inesperado."
    }

    private companion object {
        const val USERS_COLLECTION = "users"
        const val ERROR_INVALID_EMAIL = "ERROR_INVALID_EMAIL"
        const val ERROR_USER_DISABLED = "ERROR_USER_DISABLED"
    }
}