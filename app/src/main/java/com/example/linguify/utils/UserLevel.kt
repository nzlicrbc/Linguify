package com.example.linguify.utils

enum class UserLevel(val code: String, val cefrLevels: List<String>, val displayName: String) {
    BEGINNER("beginner", listOf("A1", "A2"), "Beginner"),
    INTERMEDIATE("intermediate", listOf("B1", "B2"), "Intermediate"),
    ADVANCED("advanced", listOf("C1", "C2"), "Advanced")
}