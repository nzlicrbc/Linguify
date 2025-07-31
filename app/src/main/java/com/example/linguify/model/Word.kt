package com.example.linguify.model

data class Word(
    val id: String,
    val text: String,
    val translation: String,
    val definition: String? = null,
    val example: String? = null,
    val level: String,
    val pronunciationUrl: String? = null,
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val wordType: String? = null,
    val phoneticSpelling: String? = null,
    val status: WordLearningStatus = WordLearningStatus.NEW,
    val imageUrl: String? = null
)

enum class WordLearningStatus {
    NEW,
    TO_LEARN,
    LEARNING,
    KNOWN
}