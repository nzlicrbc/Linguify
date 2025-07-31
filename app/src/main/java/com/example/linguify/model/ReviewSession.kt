package com.example.linguify.model

data class ReviewSession(
    val sessionId: String,
    val questions: List<ReviewQuestionType>,
    val startTime: Long,
    val endTime: Long? = null,
    val score: Int = 0,
    val totalQuestions: Int = questions.size
)
