package com.example.linguify.data.manager

import android.content.SharedPreferences
import com.example.linguify.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StreakManager @Inject constructor(
    private val sharedPreferences: SharedPreferences,
    private val firebaseAuth: FirebaseAuth
) {

    private val dateFormat = SimpleDateFormat(Constants.DATE_FORMAT, Locale.getDefault())

    private fun getUserSpecificKey(key: String): String {
        val userId = firebaseAuth.currentUser?.uid ?: "default"
        return "${key}_$userId"
    }

    private fun getTodayDateString(): String {
        return dateFormat.format(Date())
    }

    fun recordDailyActivity() {
        val today = getTodayDateString()
        val lastActivityDate = getLastActivityDate()

        if (lastActivityDate == today) return

        saveLastActivityDate(today)
    }

    private fun getLastActivityDate(): String {
        return sharedPreferences.getString(
            getUserSpecificKey(Constants.KEY_LAST_ACTIVITY_DATE),
            ""
        ) ?: ""
    }

    private fun saveLastActivityDate(date: String) {
        sharedPreferences.edit()
            .putString(getUserSpecificKey(Constants.KEY_LAST_ACTIVITY_DATE), date).apply()
    }
}













