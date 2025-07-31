package com.example.linguify.ui.login

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.linguify.R
import com.example.linguify.data.manager.LoginPreferencesManager
import com.example.linguify.data.repositories.AuthRepository
import com.example.linguify.utils.Constants
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.regex.Pattern
import javax.inject.Inject

sealed class LoginResult {
    data class Success(val firebaseUser: FirebaseUser) : LoginResult()
    data class Error(val message: String) : LoginResult()
    object Loading : LoginResult()
}

@HiltViewModel
class LoginViewModel @Inject constructor(
    application: Application,
    private val authRepository: AuthRepository,
    private val loginPreferencesManager: LoginPreferencesManager
) : AndroidViewModel(application) {

    private val _loginResult = MutableLiveData<LoginResult>()
    val loginResult: LiveData<LoginResult> = _loginResult

    fun isEmailValid(email: String): Boolean {
        val emailRegex = EMAIL_REGEX
        return email.isNotBlank() && Pattern.matches(emailRegex, email)
    }

    fun isPasswordValid(password: String): Boolean {
        return password.length >= MIN_PASSWORD_LENGTH
    }

    fun login(email: String, password: String, rememberMe: Boolean = false) {
        if (!isEmailValid(email)) {
            _loginResult.value = LoginResult.Error(getApplication<Application>().getString(STRING_INVALID_EMAIL))
            return
        }

        if (!isPasswordValid(password)) {
            _loginResult.value = LoginResult.Error(getApplication<Application>().getString(STRING_INVALID_PASSWORD))
            return
        }

        _loginResult.value = LoginResult.Loading

        viewModelScope.launch {
            val previousUserId = getPreviousUserId()

            val result = authRepository.login(email, password)

            result.onSuccess { firebaseUser ->
                if (previousUserId != null && previousUserId != firebaseUser.uid) {
                    clearLocalDataForUserChange()
                }

                savePreviousUserId(firebaseUser.uid)

                if (rememberMe) {
                    loginPreferencesManager.saveLoginState(email, true)
                }

                _loginResult.value = LoginResult.Success(firebaseUser)
            }.onFailure { exception ->
                _loginResult.value = LoginResult.Error(
                    exception.localizedMessage ?: getApplication<Application>().getString(STRING_LOGIN_FAILED)
                )
            }
        }
    }

    private fun getPreviousUserId(): String? {
        val prefs = getApplication<Application>().getSharedPreferences(
            Constants.PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )
        return prefs.getString("previous_user_id", null)
    }

    private fun savePreviousUserId(userId: String) {
        val prefs = getApplication<Application>().getSharedPreferences(
            Constants.PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )
        prefs.edit().putString("previous_user_id", userId).apply()
    }

    private fun clearLocalDataForUserChange() {
        val prefs = getApplication<Application>().getSharedPreferences(
            Constants.PREFERENCES_NAME,
            Context.MODE_PRIVATE
        )

        val editor = prefs.edit()

        val previousUserId = prefs.getString("previous_user_id", null)

        editor.clear()

        if (previousUserId != null) {
            editor.putString("previous_user_id", previousUserId)
        }

        editor.apply()
    }

    companion object {
        private const val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
        private const val MIN_PASSWORD_LENGTH = 6
        private val STRING_INVALID_EMAIL = R.string.invalid_email
        private val STRING_INVALID_PASSWORD = R.string.invalid_password
        private val STRING_LOGIN_FAILED = R.string.login_failed
    }
}