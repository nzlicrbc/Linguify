package com.example.linguify.data.manager

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LoginPreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey by lazy {
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
    }

    private val sharedPreferences: SharedPreferences by lazy {
        try {
            EncryptedSharedPreferences.create(
                context,
                PREFERENCES_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            context.getSharedPreferences(PREFERENCES_FILE_NAME_FALLBACK, Context.MODE_PRIVATE)
        }
    }

    suspend fun saveLoginState(email: String, isLoggedIn: Boolean) {
        withContext(Dispatchers.IO) {
            sharedPreferences.edit().apply {
                putString(KEY_EMAIL, email)
                putBoolean(KEY_IS_LOGGED_IN, isLoggedIn)
                apply()
            }
        }
    }

    fun isLoggedIn(): Boolean {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    fun getSavedEmail(): String? {
        return sharedPreferences.getString(KEY_EMAIL, null)
    }

    fun clearLoginState() {
        sharedPreferences.edit().apply {
            remove(KEY_EMAIL)
            remove(KEY_IS_LOGGED_IN)
            apply()
        }
    }

    companion object {
        private const val PREFERENCES_FILE_NAME = "login_prefs"
        private const val KEY_EMAIL = "login_email"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val PREFERENCES_FILE_NAME_FALLBACK = "login_prefs_fallback"
    }
}