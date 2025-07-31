package com.example.linguify.data.repositories

import com.example.linguify.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewRepository @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) {
    companion object {
        private const val COLLECTION_REVIEW_STATS = "review_stats"
    }

    suspend fun saveReviewStats(stats: ReviewStats) {
        firebaseAuth.currentUser?.let { user ->
            try {
                firestore.collection("users")
                    .document(user.uid)
                    .collection(COLLECTION_REVIEW_STATS)
                    .document(stats.wordId)
                    .set(stats)
                    .await()
            } catch (e: Exception) {
                throw e
            }
        }
    }

    suspend fun getReviewStats(wordId: String): ReviewStats? {
        return firebaseAuth.currentUser?.let { user ->
            try {
                val document = firestore.collection("users")
                    .document(user.uid)
                    .collection(COLLECTION_REVIEW_STATS)
                    .document(wordId)
                    .get()
                    .await()

                document.toObject(ReviewStats::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }

    suspend fun getWordsForReview(): List<String> {
        return firebaseAuth.currentUser?.let { user ->
            try {
                val now = System.currentTimeMillis()
                val documents = firestore.collection("users")
                    .document(user.uid)
                    .collection(COLLECTION_REVIEW_STATS)
                    .whereLessThanOrEqualTo("nextReviewDate", now)
                    .get()
                    .await()

                documents.documents.mapNotNull { it.id }
            } catch (e: Exception) {
                emptyList()
            }
        } ?: emptyList()
    }
}
