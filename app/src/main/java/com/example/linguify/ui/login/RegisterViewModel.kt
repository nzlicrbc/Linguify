package com.example.linguify.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.linguify.data.repositories.AuthRepository
import com.google.firebase.auth.FirebaseUser
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.regex.Pattern
import javax.inject.Inject

sealed class RegisterResult {
    data class Success(val firebaseUser: Result<FirebaseUser>) : RegisterResult()
    data class Error(val message: String) : RegisterResult()
    object Loading : RegisterResult()
}

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _registerResult = MutableLiveData<RegisterResult>()
    val registerResult: LiveData<RegisterResult> = _registerResult

    fun registerUser(username: String, email: String, password: String) {
        _registerResult.value = RegisterResult.Loading
        viewModelScope.launch {
            try {
                val user = authRepository.register(email, password)
                if (user != null) {
                    authRepository.updateUserProfile(username)
                    _registerResult.value = RegisterResult.Success(user)
                } else {
                    _registerResult.value = RegisterResult.Error(REGISTRATION_FAILED)
                }
            } catch (e: Exception) {
                _registerResult.value = RegisterResult.Error(e.message ?: UNKNOWN_ERROR)
            }
        }
    }

    companion object {
        private const val REGISTRATION_FAILED = "Registration failed"
        private const val UNKNOWN_ERROR = "An error occurred"
    }
}