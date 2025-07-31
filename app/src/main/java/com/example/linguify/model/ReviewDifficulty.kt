package com.example.linguify.model

enum class ReviewDifficulty(val label: String, val intervalMinutes: Int) {
    AGAIN("Again", 1),
    HARD("Hard", 10),
    GOOD("Good", 60),
    EASY("Easy", 5760)
}