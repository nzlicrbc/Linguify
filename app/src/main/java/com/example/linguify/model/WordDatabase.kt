package com.example.linguify.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "words",
    indices = [
        Index(value = ["cefrLevel"]),
        Index(value = ["text"])
    ]
)
data class WordEntity(
    @PrimaryKey val id: String,
    val text: String,
    val wordType: String,
    val cefrLevel: String,
    val translation: String,
    val definition: String? = null,
    val example: String? = null,
    val pronunciationUrl: String? = null
)

data class CsvWordData(
    val word: String,
    val wordType: String,
    val cefrLevel: String,
    val translation: String
)

fun getCefrLevelToUserLevel(cefrLevel: String): String {
    return when(cefrLevel) {
        "A1", "A2" -> "beginner"
        "B1", "B2" -> "intermediate"
        "C1", "C2" -> "advanced"
        else -> "beginner"
    }
}
