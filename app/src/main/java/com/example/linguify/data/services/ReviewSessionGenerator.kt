package com.example.linguify.data.services

import android.util.Log
import com.example.linguify.model.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewSessionGenerator @Inject constructor(
    private val questionGeneratorService: QuestionGeneratorService
) {
    suspend fun generateReviewQuestions(word: Word): List<ReviewQuestionType> {
        Log.d("ReviewSessionGenerator", "Generating single question for word: ${word.text}")
        return questionGeneratorService.generateReviewQuestions(word)
    }

    suspend fun generateMixedReviewQuestions(words: List<Word>): List<ReviewQuestionType> {
        Log.d("ReviewSessionGenerator", "Generating mixed questions for ${words.size} words")

        if (words.isEmpty()) {
            Log.w("ReviewSessionGenerator", "No words provided for question generation")
            return emptyList()
        }

        val questions = questionGeneratorService.generateMixedReviewQuestions(words)

        Log.d("ReviewSessionGenerator", "Generated ${questions.size} questions from ${words.size} words")

        return questions.shuffled()
    }

    suspend fun generateQuestionSet(words: List<Word>, maxQuestions: Int = 10): List<ReviewQuestionType> {
        val selectedWords = if (words.size > maxQuestions) {
            words.shuffled().take(maxQuestions)
        } else {
            words
        }

        Log.d("ReviewSessionGenerator", "Generating question set for ${selectedWords.size} selected words")

        return generateMixedReviewQuestions(selectedWords)
    }
}