package com.example.linguify.data.manager

import android.content.SharedPreferences
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class StartupManager @Inject constructor(
    @Named("onboardingPrefs") private val onboardingPrefs: SharedPreferences,
    private val loginPreferencesManager: LoginPreferencesManager
) {

    companion object {
        private const val KEY_FIRST_TIME_USER = "first_time_user"
    }

    fun isFirstTimeUser(): Boolean {
        return onboardingPrefs.getBoolean(KEY_FIRST_TIME_USER, true)
    }

    fun markFirstTimeDone() {
        onboardingPrefs.edit().putBoolean(KEY_FIRST_TIME_USER, false).apply()
    }

    fun shouldShowOnboarding(): Boolean {
        return isFirstTimeUser() && loginPreferencesManager.isLoggedIn()
    }

    fun shouldShowLevelTest(): Boolean {
        return !isFirstTimeUser() && loginPreferencesManager.isLoggedIn()
    }
}