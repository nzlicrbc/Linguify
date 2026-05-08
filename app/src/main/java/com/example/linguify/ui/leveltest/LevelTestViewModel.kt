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

    private val _currentQuestionIndex = MutableStateFlow(0)
    val currentQuestionIndex: StateFlow<Int> = _currentQuestionIndex

    private val _quizCompleted = MutableStateFlow<UserLevel?>(null)
    val quizCompleted: StateFlow<UserLevel?> = _quizCompleted

    private var score = 0

    fun loadTestQuestions() {
        viewModelScope.launch {
            _testQuestions.value = TestQuestionsState.Loading
            try {
                val questions = levelTestRepository.getTestQuestions()
                _currentQuestionIndex.value = 0
                score = 0
                _quizCompleted.value = null
                _testQuestions.value = TestQuestionsState.Success(questions)
            } catch (e: Exception) {
                _testQuestions.value = TestQuestionsState.Error("An error occurred while loading questions: ${e.message}")
            }
        }
    }

    fun answerQuestion(selectedOptionIndex: Int) {
        val state = _testQuestions.value as? TestQuestionsState.Success ?: return
        val questions = state.questions
        val index = _currentQuestionIndex.value

        if (index >= questions.size) return

        if (selectedOptionIndex == questions[index].correctOptionIndex) {
            score++
        }

        if (index + 1 < questions.size) {
            _currentQuestionIndex.value = index + 1
        } else {
            val userLevel = when {
                score < questions.size * 0.3 -> UserLevel.BEGINNER
                score < questions.size * 0.7 -> UserLevel.INTERMEDIATE
                else -> UserLevel.ADVANCED
            }
            _quizCompleted.value = userLevel
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
