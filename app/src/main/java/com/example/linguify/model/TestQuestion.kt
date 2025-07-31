package com.example.linguify.model

data class TestQuestion(
    val id: String,
    val questionText: String,
    val options: List<String>,
    val correctOptionIndex: Int,
    val difficulty: QuestionDifficulty
)

enum class QuestionDifficulty {
    EASY, MEDIUM, HARD
}
