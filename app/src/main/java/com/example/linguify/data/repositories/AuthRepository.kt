package com.example.linguify.data.repositories

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepository @Inject constructor(
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) {
    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user
                ?: return Result.failure(Exception("Login succeeded but Firebase returned no user"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun register(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user
                ?: return Result.failure(Exception("Registration succeeded but Firebase returned no user"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Result<Unit> {
        return try {
            firebaseAuth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(username: String): Result<Unit> {
        val user = firebaseAuth.currentUser
        return try {
            user?.let {
                val userData = hashMapOf(
                    FIELD_USERNAME to username,
                    FIELD_EMAIL to user.email,
                    FIELD_CREATED_AT to System.currentTimeMillis()
                )

                firestore.collection(COLLECTION_USERS)
                    .document(user.uid)
                    .set(userData)
                    .await()
                Result.success(Unit)
            } ?: Result.failure(Exception(ERROR_NO_USER))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    fun logout(): Boolean {
        return try {
            firebaseAuth.signOut()
            true
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        private const val COLLECTION_USERS = "users"
        private const val FIELD_USERNAME = "username"
        private const val FIELD_EMAIL = "email"
        private const val FIELD_CREATED_AT = "createdAt"
        private const val ERROR_NO_USER = "No authenticated user found"
    }
}
