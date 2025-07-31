package com.example.linguify.model

sealed class ReviewQuestionType {
    abstract val word: Word
    abstract val type: String

    data class MultipleChoice(
        override val word: Word,
        val questionText: String,
        val options: List<String>,
        val correctIndex: Int,
        override val type: String = "multiple_choice"
    ) : ReviewQuestionType()

    data class ContextSentence(
        override val word: Word,
        val sentence: String,
        val options: List<String>,
        val correctIndex: Int,
        override val type: String = "context_sentence"
    ) : ReviewQuestionType()

    data class Definition(
        override val word: Word,
        val definitions: List<String>,
        val correctIndex: Int,
        override val type: String = "definition"
    ) : ReviewQuestionType()
}
