package com.example.linguify.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import android.content.SharedPreferences
import javax.inject.Named

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @Named("onboardingPrefs") private val sharedPreferences: SharedPreferences
) : ViewModel() {

    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }

    fun setOnboardingCompleted() {
        viewModelScope.launch {
            sharedPreferences.edit()
                .putBoolean(KEY_ONBOARDING_COMPLETED, true)
                .apply()
        }
    }
}