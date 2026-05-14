package com.example.linguify.model

// intervalMinutes is kept as a fallback label only.
// Actual scheduling uses SM-2 (interval + easeFactor) in ReviewViewModel.
enum class ReviewDifficulty(val label: String, val intervalMinutes: Int) {
    AGAIN("Again", 10),
    HARD("Hard", 1440),
    GOOD("Good", 4320),
    EASY("Easy", 20160)
}
