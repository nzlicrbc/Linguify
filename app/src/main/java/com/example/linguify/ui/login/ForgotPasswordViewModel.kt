package com.example.linguify.ui.login

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.linguify.R
import com.example.linguify.data.repositories.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class ResetPasswordResult {
    data class Success(val message: String) : ResetPasswordResult()
    data class Error(val message: String) : ResetPasswordResult()
    object Loading : ResetPasswordResult()
}

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    application: Application,
    private val authRepository: AuthRepository
) :AndroidViewModel(application) {

    private val _resetResult = MutableLiveData<ResetPasswordResult>()
    val resetResult: LiveData<ResetPasswordResult> = _resetResult

    fun isEmailValid(email: String): Boolean {
        val emailRegex = EMAIL_REGEX
        return email.isNotBlank() && email.matches(emailRegex.toRegex())
    }

    fun resetPassword(email: String) {
        _resetResult.value = ResetPasswordResult.Loading
        viewModelScope.launch {
            try {
                authRepository.sendPasswordResetEmail(email)
                _resetResult.value =
                    ResetPasswordResult.Success(getApplication<Application>().getString(SUCCESS_MESSAGE))
            } catch (e: Exception) {
                _resetResult.value = ResetPasswordResult.Error(e.message ?:getApplication<Application>().getString(PASSWORD_RESET_ERROR))
            }
        }
    }

    companion object {
        private const val EMAIL_REGEX = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$"
        private val SUCCESS_MESSAGE = R.string.forgot_password_email_sent
        private val PASSWORD_RESET_ERROR = R.string.password_reset_error
    }
}