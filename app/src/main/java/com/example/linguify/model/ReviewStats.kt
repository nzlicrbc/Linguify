package com.example.linguify.model

data class ReviewStats(
    val wordId: String,
    val lastReviewDate: Long,
    val nextReviewDate: Long,
    val correctAnswers: Int,
    val incorrectAnswers: Int,
    val difficulty: ReviewDifficulty = ReviewDifficulty.AGAIN
)
