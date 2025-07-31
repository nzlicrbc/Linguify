package com.example.linguify.model

data class WordDetail(
    val id: String,
    val text: String,
    val translation: String,
    val definition: String? = null,
    val phoneticSpelling: String? = null,
    val pronunciationUrl: String? = null,
    val examples: List<String> = emptyList(),
    val synonyms: List<String> = emptyList(),
    val antonyms: List<String> = emptyList(),
    val wordType: String? = null,
    val level: String,
    val status: WordLearningStatus = WordLearningStatus.NEW
)
