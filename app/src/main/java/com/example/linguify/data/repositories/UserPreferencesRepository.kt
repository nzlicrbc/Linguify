package com.example.linguify.data.repositories

import android.content.SharedPreferences
import com.example.linguify.utils.UserLevel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth
) {
    companion object {
        private const val KEY_USER_LEVEL = "user_level"
        private const val KEY_DAILY_WORD_TARGET = "daily_word_target"
        private const val KEY_NEXT_WORD_TO_LEARN = "next_word_to_learn"
    }

    private fun getUserSpecificKey(key: String): String {
        val userId = firebaseAuth.currentUser?.uid ?: "default"
        return "${key}_$userId"
    }

    suspend fun saveUserLevel(userLevel: UserLevel) {
        sharedPreferences.edit()
            .putString(getUserSpecificKey(KEY_USER_LEVEL), userLevel.code)
            .apply()

        firebaseAuth.currentUser?.let { user ->
            val userData = hashMapOf(
                "level" to userLevel.code,
                "lastUpdated" to System.currentTimeMillis()
            )

            firestore.collection("users")
                .document(user.uid)
                .set(userData as Map<String, Any>, SetOptions.merge())
                .await()
        }
    }

    suspend fun getUserLevel(): UserLevel {
        val localLevel = sharedPreferences.getString(getUserSpecificKey(KEY_USER_LEVEL), null)

        if (localLevel != null) {
            return UserLevel.values().find { it.code == localLevel } ?: UserLevel.BEGINNER
        }

        return firebaseAuth.currentUser?.let { user ->
            try {
                val userDoc = firestore.collection("users")
                    .document(user.uid)
                    .get()
                    .await()

                val userLevelStr = userDoc.getString("level")
                val level = UserLevel.values().find { it.code == userLevelStr } ?: UserLevel.BEGINNER

                sharedPreferences.edit()
                    .putString(getUserSpecificKey(KEY_USER_LEVEL), level.code)
                    .apply()

                level
            } catch (e: Exception) {
                UserLevel.BEGINNER
            }
        } ?: UserLevel.BEGINNER
    }

    suspend fun saveDailyWordTarget(target: Int) {
        sharedPreferences.edit()
            .putInt(getUserSpecificKey(KEY_DAILY_WORD_TARGET), target)
            .apply()

        firebaseAuth.currentUser?.let { user ->
            val userData = hashMapOf(
                "dailyWordTarget" to target,
                "lastUpdated" to System.currentTimeMillis()
            )

            firestore.collection("users")
                .document(user.uid)
                .update(userData as Map<String, Any>)
                .await()
        }
    }

    suspend fun getDailyWordTarget(): Int {
        val localTarget = sharedPreferences.getInt(getUserSpecificKey(KEY_DAILY_WORD_TARGET), 0)

        if (localTarget > 0) {
            return localTarget
        }

        return firebaseAuth.currentUser?.let { user ->
            try {
                val userDoc = firestore.collection("users")
                    .document(user.uid)
                    .get()
                    .await()

                val target = userDoc.getLong("dailyWordTarget")?.toInt() ?: 5

                sharedPreferences.edit()
                    .putInt(KEY_DAILY_WORD_TARGET, target)
                    .apply()

                target
            } catch (e: Exception) {
                5
            }
        } ?: 5
    }

    suspend fun saveNextWordToLearn(word: String) {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit()
                .putString(getUserSpecificKey(KEY_NEXT_WORD_TO_LEARN), word)
                .apply()

            firebaseAuth.currentUser?.let { user ->
                try {
                    val userData = hashMapOf(
                        "nextWordToLearn" to word,
                        "lastUpdated" to System.currentTimeMillis()
                    )

                    firestore.collection("users")
                        .document(user.uid)
                        .update(userData as Map<String, Any>)
                        .await()
                } catch (e: Exception) {
                    val userData = hashMapOf(
                        "nextWordToLearn" to word,
                        "lastUpdated" to System.currentTimeMillis()
                    )

                    firestore.collection("users")
                        .document(user.uid)
                        .set(userData, SetOptions.merge())
                        .await()
                }
            }
        }
    }

    suspend fun getNextWordToLearn(): String {
        val localNextWord = sharedPreferences.getString(getUserSpecificKey(KEY_NEXT_WORD_TO_LEARN), "")

        if (!localNextWord.isNullOrEmpty()) {
            return localNextWord
        }

        return withContext(Dispatchers.IO) {
            firebaseAuth.currentUser?.let { user ->
                try {
                    val userDoc = firestore.collection("users")
                        .document(user.uid)
                        .get()
                        .await()

                    val nextWord = userDoc.getString("nextWordToLearn") ?: ""

                    sharedPreferences.edit()
                        .putString(getUserSpecificKey(KEY_NEXT_WORD_TO_LEARN), nextWord)
                        .apply()

                    nextWord
                } catch (e: Exception) {
                    ""
                }
            } ?: ""
        }
    }
}
