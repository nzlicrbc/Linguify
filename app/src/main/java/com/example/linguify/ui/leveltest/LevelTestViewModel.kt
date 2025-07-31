package com.example.linguify.ui.leveltest

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.linguify.data.repositories.UserPreferencesRepository
import com.example.linguify.model.TestQuestion
import com.example.linguify.utils.UserLevel
import com.example.linguify.data.repositories.LevelTestRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LevelTestViewModel @Inject constructor(
    private val levelTestRepository: LevelTestRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _testQuestions = MutableStateFlow<TestQuestionsState>(TestQuestionsState.Initial)
    val testQuestions: StateFlow<TestQuestionsState> = _testQuestions

    private val _levelSaveState = MutableStateFlow<LevelSaveState>(LevelSaveState.Initial)
    val levelSaveState: StateFlow<LevelSaveState> = _levelSaveState

    fun loadTestQuestions() {
        viewModelScope.launch {
            _testQuestions.value = TestQuestionsState.Loading
            try {
                val questions = levelTestRepository.getTestQuestions()
                _testQuestions.value = TestQuestionsState.Success(questions)
            } catch (e: Exception) {
                _testQuestions.value = TestQuestionsState.Error("An error occurred while loading questions: ${e.message}")
            }
        }
    }

    fun saveUserLevel(userLevel: UserLevel) {
        viewModelScope.launch {
            _levelSaveState.value = LevelSaveState.Loading
            try {
                userPreferencesRepository.saveUserLevel(userLevel)
                _levelSaveState.value = LevelSaveState.Success
            } catch (e: Exception) {
                _levelSaveState.value = LevelSaveState.Error("An error occurred while saving level: ${e.message}")
            }
        }
    }

    sealed class TestQuestionsState {
        object Initial : TestQuestionsState()
        object Loading : TestQuestionsState()
        data class Success(val questions: List<TestQuestion>) : TestQuestionsState()
        data class Error(val message: String) : TestQuestionsState()
    }

    sealed class LevelSaveState {
        object Initial : LevelSaveState()
        object Loading : LevelSaveState()
        object Success : LevelSaveState()
        data class Error(val message: String) : LevelSaveState()
    }
}
