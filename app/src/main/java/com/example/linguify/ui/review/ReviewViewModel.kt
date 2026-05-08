package com.example.linguify.ui.review

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.linguify.data.repositories.ReviewRepository
import com.example.linguify.data.repositories.WordRepository
import com.example.linguify.data.services.ReviewSessionGenerator
import com.example.linguify.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ReviewViewModel @Inject constructor(
    private val wordRepository: WordRepository,
    private val reviewRepository: ReviewRepository,
    private val reviewSessionGenerator: ReviewSessionGenerator
) : ViewModel() {

    private val _reviewState = MutableStateFlow<ReviewState>(ReviewState.Loading)
    val reviewState: StateFlow<ReviewState> = _reviewState

    private var currentSession: ReviewSession? = null
    private var correctAnswers = 0
    private val maxQuestionsPerSession = 10
    private var loadingJob: Job? = null

    fun loadReviewSession() {
        loadingJob?.cancel()
        loadingJob = viewModelScope.launch {
            _reviewState.value = ReviewState.Loading

            try {
                var wordsToReview = wordRepository.getSavedWordsFromFirebase()
                    .filter { it.status == WordLearningStatus.LEARNING }

                if (wordsToReview.isEmpty()) {
                    wordsToReview = wordRepository.getSavedWordsFromFirebase()
                        .filter { it.status == WordLearningStatus.TO_LEARN }
                }

                if (wordsToReview.isEmpty()) {
                    _reviewState.value = ReviewState.Error("No words available for review. Please add some words to learn first.")
                    return@launch
                }

                Log.d("ReviewViewModel", "Found ${wordsToReview.size} words available for review")

                val selectedWords = selectWordsForReview(wordsToReview, maxQuestionsPerSession)

                Log.d("ReviewViewModel", "Selected ${selectedWords.size} words for this review session using smart selection")

                val questions = reviewSessionGenerator.generateMixedReviewQuestions(selectedWords)

                if (questions.isEmpty()) {
                    if (selectedWords.size > 3) {
                        Log.w("ReviewViewModel", "No questions generated, trying with fewer words...")
                        val fewerWords = selectedWords.take(3)
                        val retryQuestions = reviewSessionGenerator.generateMixedReviewQuestions(fewerWords)

                        if (retryQuestions.isNotEmpty()) {
                            Log.d("ReviewViewModel", "Successfully generated ${retryQuestions.size} questions with fewer words")
                            createAndStartSession(retryQuestions)
                            return@launch
                        }
                    }

                    _reviewState.value = ReviewState.Error("Could not generate questions due to API limitations. Please try again in a few minutes.")
                    return@launch
                }

                val successfulWordIds = questions.map { it.word.id }.toSet()
                val failedWords = selectedWords.filter { it.id !in successfulWordIds }

                if (failedWords.isNotEmpty()) {
                    Log.w("ReviewViewModel", "Failed to generate questions for: ${failedWords.map { it.text }}")
                }

                Log.d("ReviewViewModel", "Successfully generated ${questions.size} questions")

                val questionTypeCount = questions.groupBy { it.type }.mapValues { it.value.size }
                Log.d("ReviewViewModel", "Question types: $questionTypeCount")

                createAndStartSession(questions)

            } catch (e: Exception) {
                Log.e("ReviewViewModel", "Error loading review session: ${e.message}", e)
                _reviewState.value = ReviewState.Error("An error occurred while loading the review session: ${e.message}")
            }
        }
    }

    private fun createAndStartSession(questions: List<ReviewQuestionType>) {
        val session = ReviewSession(
            sessionId = UUID.randomUUID().toString(),
            questions = questions,
            startTime = System.currentTimeMillis()
        )

        currentSession = session
        correctAnswers = 0
        _reviewState.value = ReviewState.SessionReady(session)
    }

    private suspend fun selectWordsForReview(allWords: List<Word>, maxCount: Int): List<Word> {
        if (allWords.size <= maxCount) {
            return allWords.shuffled()
        }

        val selectedWords = mutableListOf<Word>()
        val now = System.currentTimeMillis()

        val wordsWithStats = allWords.map { word ->
            val stats = try {
                reviewRepository.getReviewStats(word.id)
            } catch (e: Exception) {
                null
            }
            Pair(word, stats)
        }

        val wordsForReview = wordsWithStats.filter { (_, stats) ->
            stats != null && stats.nextReviewDate <= now
        }.map { it.first }

        selectedWords.addAll(wordsForReview.shuffled().take(maxCount - selectedWords.size))
        Log.d("ReviewViewModel", "Added ${wordsForReview.size} words due for review")

        if (selectedWords.size < maxCount) {
            val neverReviewed = wordsWithStats.filter { (_, stats) ->
                stats == null
            }.map { it.first }

            val toAdd = (maxCount - selectedWords.size).coerceAtMost(neverReviewed.size)
            selectedWords.addAll(neverReviewed.shuffled().take(toAdd))
            Log.d("ReviewViewModel", "Added $toAdd never-reviewed words")
        }

        if (selectedWords.size < maxCount) {
            val mostMistakes = wordsWithStats
                .filter { (word, stats) ->
                    !selectedWords.contains(word) && stats != null
                }
                .sortedByDescending { (_, stats) ->
                    stats!!.incorrectAnswers
                }
                .map { it.first }

            val toAdd = (maxCount - selectedWords.size).coerceAtMost(mostMistakes.size)
            selectedWords.addAll(mostMistakes.take(toAdd))
            Log.d("ReviewViewModel", "Added $toAdd words with most mistakes")
        }

        if (selectedWords.size < maxCount) {
            val remaining = allWords.filter { !selectedWords.contains(it) }
            val toAdd = maxCount - selectedWords.size
            selectedWords.addAll(remaining.shuffled().take(toAdd))
            Log.d("ReviewViewModel", "Added $toAdd remaining words randomly")
        }

        return selectedWords.shuffled()
    }

    fun submitAnswer(questionIndex: Int, answer: Any) {
        viewModelScope.launch {
            val session = currentSession ?: return@launch

            if (questionIndex >= session.questions.size) {
                Log.e("ReviewViewModel", "Invalid question index: $questionIndex")
                return@launch
            }

            val question = session.questions[questionIndex]
            val isCorrect = validateAnswer(question, answer)

            if (isCorrect) {
                correctAnswers++
                Log.d("ReviewViewModel", "Correct answer for ${question.word.text} (${question.type})")
            } else {
                Log.d("ReviewViewModel", "Incorrect answer for ${question.word.text} (${question.type})")
            }

            updateWordReviewStats(question.word, isCorrect)

            _reviewState.value = ReviewState.QuestionAnswered(isCorrect)
        }
    }

    private fun validateAnswer(question: ReviewQuestionType, answer: Any): Boolean {
        return try {
            when (question) {
                is ReviewQuestionType.MultipleChoice -> {
                    val selectedIndex = answer as? Int ?: return false
                    selectedIndex == question.correctIndex
                }
                is ReviewQuestionType.ContextSentence -> {
                    val selectedIndex = answer as? Int ?: return false
                    selectedIndex == question.correctIndex
                }
                is ReviewQuestionType.Definition -> {
                    val selectedIndex = answer as? Int ?: return false
                    selectedIndex == question.correctIndex
                }
            }
        } catch (e: Exception) {
            Log.e("ReviewViewModel", "Error validating answer: ${e.message}")
            false
        }
    }

    private suspend fun updateWordReviewStats(word: Word, isCorrect: Boolean) {
        try {
            val existingStats = reviewRepository.getReviewStats(word.id)

            val updatedStats = if (existingStats != null) {
                if (isCorrect) {
                    existingStats.copy(
                        lastReviewDate = System.currentTimeMillis(),
                        nextReviewDate = calculateNextReviewDate(existingStats.difficulty),
                        correctAnswers = existingStats.correctAnswers + 1,
                        difficulty = improveDifficulty(existingStats.difficulty)
                    )
                } else {
                    existingStats.copy(
                        lastReviewDate = System.currentTimeMillis(),
                        nextReviewDate = calculateNextReviewDate(ReviewDifficulty.AGAIN),
                        incorrectAnswers = existingStats.incorrectAnswers + 1,
                        difficulty = ReviewDifficulty.AGAIN
                    )
                }
            } else {
                ReviewStats(
                    wordId = word.id,
                    lastReviewDate = System.currentTimeMillis(),
                    nextReviewDate = calculateNextReviewDate(if (isCorrect) ReviewDifficulty.GOOD else ReviewDifficulty.AGAIN),
                    correctAnswers = if (isCorrect) 1 else 0,
                    incorrectAnswers = if (isCorrect) 0 else 1,
                    difficulty = if (isCorrect) ReviewDifficulty.GOOD else ReviewDifficulty.AGAIN
                )
            }

            reviewRepository.saveReviewStats(updatedStats)
            Log.d("ReviewViewModel", "Updated review stats for ${word.text}: correct=${isCorrect}, difficulty=${updatedStats.difficulty}")

        } catch (e: Exception) {
            Log.e("ReviewViewModel", "Error updating review stats for ${word.text}: ${e.message}")
        }
    }

    private fun improveDifficulty(currentDifficulty: ReviewDifficulty): ReviewDifficulty {
        return when (currentDifficulty) {
            ReviewDifficulty.AGAIN -> ReviewDifficulty.HARD
            ReviewDifficulty.HARD -> ReviewDifficulty.GOOD
            ReviewDifficulty.GOOD -> ReviewDifficulty.EASY
            ReviewDifficulty.EASY -> ReviewDifficulty.EASY
        }
    }

    private fun calculateNextReviewDate(difficulty: ReviewDifficulty): Long {
        val now = System.currentTimeMillis()
        val intervalMillis = difficulty.intervalMinutes * 60 * 1000L
        return now + intervalMillis
    }

    fun skipQuestion(questionIndex: Int) {
        viewModelScope.launch {
            val session = currentSession ?: return@launch
            if (questionIndex >= session.questions.size) return@launch

            val question = session.questions[questionIndex]
            updateWordReviewStats(question.word, isCorrect = false)

            _reviewState.value = ReviewState.QuestionAnswered(isCorrect = false)
        }
    }

    fun completeSession() {
        val session = currentSession ?: return

        val completedSession = session.copy(
            endTime = System.currentTimeMillis(),
            score = correctAnswers
        )

        Log.d("ReviewViewModel", "Session completed: ${correctAnswers}/${session.questions.size} correct")

        _reviewState.value = ReviewState.SessionCompleted(completedSession)
    }

    sealed class ReviewState {
        object Loading : ReviewState()
        data class SessionReady(val session: ReviewSession) : ReviewState()
        data class QuestionAnswered(val isCorrect: Boolean) : ReviewState()
        data class SessionCompleted(val session: ReviewSession) : ReviewState()
        data class Error(val message: String) : ReviewState()
    }
}