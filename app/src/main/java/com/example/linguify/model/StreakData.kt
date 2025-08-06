package com.example.linguify.model

data class StreakData (
    val streakDays: List<Boolean> = List(7) {false},
    val currentStreakCount: Int = 0,
    val lastActivityDate: String = ""
)
