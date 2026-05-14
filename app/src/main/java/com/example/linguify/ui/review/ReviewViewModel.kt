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
import kotlin.math.roundToInt

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
    private var questionAnsweredSeq = 0L

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
        questionAnsweredSeq = 0L
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

            _reviewState.value = ReviewState.QuestionAnswered(isCorrect, ++questionAnsweredSeq)
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
            val now = System.currentTimeMillis()
            val existing = reviewRepository.getReviewStats(word.id)

            val currentInterval = existing?.interval ?: 0
            val currentEase = existing?.easeFactor ?: 2.5

            val (newInterval, newEase) = if (isCorrect) {
                val ni = when (currentInterval) {
                    0 -> 1
                    1 -> 6
                    else -> (currentInterval * currentEase).roundToInt()
                }
                Pair(ni, currentEase)
            } else {
                Pair(0, (currentEase - 0.2).coerceAtLeast(1.3))
            }

            val nextReviewMillis = if (newInterval == 0) {
                now + 10 * 60 * 1000L
            } else {
                now + newInterval * 24 * 60 * 60 * 1000L
            }

            val newDifficulty = when {
                !isCorrect -> ReviewDifficulty.AGAIN
                newInterval <= 1 -> ReviewDifficulty.HARD
                newInterval <= 6 -> ReviewDifficulty.GOOD
                else -> ReviewDifficulty.EASY
            }

            val updatedStats = ReviewStats(
                wordId = word.id,
                lastReviewDate = now,
                nextReviewDate = nextReviewMillis,
                correctAnswers = (existing?.correctAnswers ?: 0) + if (isCorrect) 1 else 0,
                incorrectAnswers = (existing?.incorrectAnswers ?: 0) + if (!isCorrect) 1 else 0,
                difficulty = newDifficulty,
                interval = newInterval,
                easeFactor = newEase
            )

            reviewRepository.saveReviewStats(updatedStats)
            Log.d("ReviewViewModel", "SM-2 update for ${word.text}: correct=$isCorrect, interval=${newInterval}d, ease=%.2f, next=${newDifficulty.label}".format(newEase))

        } catch (e: Exception) {
            Log.e("ReviewViewModel", "Error updating review stats for ${word.text}: ${e.message}")
        }
    }

    fun skipQuestion(questionIndex: Int) {
        viewModelScope.launch {
            val session = currentSession ?: return@launch
            if (questionIndex >= session.questions.size) return@launch

            val question = session.questions[questionIndex]
            updateWordReviewStats(question.word, isCorrect = false)

            _reviewState.value = ReviewState.QuestionAnswered(isCorrect = false, seq = ++questionAnsweredSeq)
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
        data class QuestionAnswered(val isCorrect: Boolean, val seq: Long) : ReviewState()
        data class SessionCompleted(val session: ReviewSession) : ReviewState()
        data class Error(val message: String) : ReviewState()
    }
}