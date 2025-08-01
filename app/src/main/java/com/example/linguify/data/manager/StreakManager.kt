package com.example.linguify.data.manager

import android.content.SharedPreferences
import com.example.linguify.utils.Constants
import com.google.firebase.auth.FirebaseAuth
import java.text.SimpleDateFormat
import java.util.Calendar
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

        val currentStreak = getCurrentStreak()
        val newStreak = calculateNewStreak(lastActivityDate, today, currentStreak)

        saveCurrentStreak(newStreak)
        saveLastActivityDate(today)
        updateWeeklyStreakData(today)
    }

    fun getLastActivityDate(): String {
        return sharedPreferences.getString(
            getUserSpecificKey(Constants.KEY_LAST_ACTIVITY_DATE),
            ""
        ) ?: ""
    }

    private fun saveLastActivityDate(date: String) {
        sharedPreferences.edit()
            .putString(getUserSpecificKey(Constants.KEY_LAST_ACTIVITY_DATE), date).apply()
    }

    private fun calculateNewStreak(lastDate: String, today: String, currentStreak: Int): Int {
        if (lastDate.isEmpty()) return 1
        return if (isConsecutiveDay(lastDate, today)) {
            currentStreak + 1
        } else {
            1
        }
    }

    private fun isConsecutiveDay(lastDate: String, today: String): Boolean {
        if (lastDate.isEmpty()) return false

        try {
            val lastDateObj = dateFormat.parse(lastDate) ?: return false
            val todayDateObj = dateFormat.parse(today) ?: return false

            val diffInMillis = todayDateObj.time - lastDateObj.time
            val diffInDays = diffInMillis / (24 * 60 * 60 * 1000)

            return diffInDays == 1L
        } catch (e: Exception) {
            return false

        }
    }

    fun getCurrentStreak(): Int {
        return sharedPreferences.getInt(
            getUserSpecificKey(Constants.KEY_CURRENT_STREAK),
            0
        )
    }

    private fun saveCurrentStreak(streak: Int) {
        sharedPreferences.edit()
            .putInt(getUserSpecificKey(Constants.KEY_CURRENT_STREAK), streak).apply()
    }

    private fun updateWeeklyStreakData(today: String) {
        val streakDays = getStoredStreakDays().toMutableSet()
        streakDays.add(today)

        cleanOldStreakDays(streakDays)

        saveStreakDays(streakDays)
    }

    fun getWeeklyStreak(): List<Boolean> {
        val streakDays = getStoredStreakDays()
        val weeklyStreak = mutableListOf<Boolean>()

        for (i in 6 downTo 0) {
            val dateString = getDateStringForDaysAgo(i)
            weeklyStreak.add(streakDays.contains(dateString))
        }

        return weeklyStreak
    }

    private fun getStoredStreakDays(): Set<String> {
        val streakDaysString = sharedPreferences.getString(
            getUserSpecificKey(Constants.KEY_STREAK_DAYS),
            ""
        ) ?: ""
        return if (streakDaysString.isNotEmpty()) {
            streakDaysString.split(",").toSet()
        } else {
            emptySet()
        }
    }

    private fun saveStreakDays(streakDays: Set<String>) {
        val streakDaysString = streakDays.joinToString(",")
        sharedPreferences.edit()
            .putString(getUserSpecificKey(Constants.KEY_STREAK_DAYS), streakDaysString).apply()
    }

    private fun getDateStringForDaysAgo(daysAgo: Int): String {
        val calender = Calendar.getInstance()
        calender.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return dateFormat.format(calender.time)
    }

    private fun cleanOldStreakDays(streakDays: MutableSet<String>) {
        val sevenDaysAgo = getDateStringForDaysAgo(7)
        val sevenDaysAgoDate = dateFormat.parse(sevenDaysAgo)?.time ?: return
        streakDays.removeAll { dateString ->
            try {
                val date = dateFormat.parse(dateString)?.time ?: return@removeAll false
                date < sevenDaysAgoDate
            } catch (e: Exception) {
                true
            }
        }
    }
}













