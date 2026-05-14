package com.example.linguify.model

data class ReviewStats(
    val wordId: String = "",
    val lastReviewDate: Long = 0L,
    val nextReviewDate: Long = 0L,
    val correctAnswers: Int = 0,
    val incorrectAnswers: Int = 0,
    val difficulty: ReviewDifficulty = ReviewDifficulty.AGAIN,
    val interval: Int = 0,
    val easeFactor: Double = 2.5
)
