package com.example.linguify.data.repositories

import com.example.linguify.model.QuestionDifficulty
import com.example.linguify.model.TestQuestion
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LevelTestRepository @Inject constructor(
    private val firestore: FirebaseFirestore
) {
    suspend fun getTestQuestions(): List<TestQuestion> {
        val questionsCollection = firestore.collection("level_test_questions")

        return try {
            val snapshot = questionsCollection.get().await()
            val questions = snapshot.documents.mapNotNull { document ->
                val id = document.id
                val questionText = document.getString("questionText") ?: return@mapNotNull null
                val optionsRaw = document.get("options") as? ArrayList<*>
                val options = optionsRaw?.filterIsInstance<String>()
                if (options == null || options.size != (optionsRaw?.size ?: 0)) {
                    return@mapNotNull null
                }
                val correctOptionIndex = document.getLong("correctOptionIndex")?.toInt() ?: return@mapNotNull null
                val difficultyString = document.getString("difficulty") ?: "MEDIUM"
                val difficulty = try {
                    QuestionDifficulty.valueOf(difficultyString)
                } catch (e: IllegalArgumentException) {
                    QuestionDifficulty.MEDIUM
                }

                TestQuestion(id, questionText, options, correctOptionIndex, difficulty)
            }

            if (questions.isEmpty()) {
                getDefaultTestQuestions()
            } else {
                questions
            }
        } catch (e: Exception) {
            getDefaultTestQuestions()
        }
    }

    private fun getDefaultTestQuestions(): List<TestQuestion> {
        return listOf(
            TestQuestion(
                id = "q1",
                questionText = "What ___ your name?",
                options = listOf("is", "are", "am", "be"),
                correctOptionIndex = 0,
                difficulty = QuestionDifficulty.EASY
            ),
            TestQuestion(
                id = "q2",
                questionText = "She ___ to the cinema yesterday.",
                options = listOf("go", "goes", "went", "going"),
                correctOptionIndex = 2,
                difficulty = QuestionDifficulty.EASY
            ),
            TestQuestion(
                id = "q3",
                questionText = "They ___ football every Sunday.",
                options = listOf("play", "plays", "playing", "are play"),
                correctOptionIndex = 0,
                difficulty = QuestionDifficulty.EASY
            ),
            TestQuestion(
                id = "q4",
                questionText = "I ___ never ___ to Paris.",
                options = listOf("have / been", "has / been", "have / went", "has / went"),
                correctOptionIndex = 0,
                difficulty = QuestionDifficulty.MEDIUM
            ),
            TestQuestion(
                id = "q5",
                questionText = "If it ___ tomorrow, we ___ to the beach.",
                options = listOf(
                    "rain / won't go",
                    "rains / won't go",
                    "will rain / don't go",
                    "raining / not going"
                ),
                correctOptionIndex = 1,
                difficulty = QuestionDifficulty.MEDIUM
            ),
            TestQuestion(
                id = "q6",
                questionText = "The book ___ by John last year.",
                options = listOf("write", "wrote", "was written", "written"),
                correctOptionIndex = 2,
                difficulty = QuestionDifficulty.MEDIUM
            ),
            TestQuestion(
                id = "q7",
                questionText = "She ___ for the company for five years before she got promoted.",
                options = listOf(
                    "works",
                    "worked",
                    "has worked",
                    "had been working"
                ),
                correctOptionIndex = 3,
                difficulty = QuestionDifficulty.HARD
            ),
            TestQuestion(
                id = "q8",
                questionText = "I wish I ___ harder for the exam.",
                options = listOf("study", "studied", "had studied", "would study"),
                correctOptionIndex = 2,
                difficulty = QuestionDifficulty.HARD
            ),
            TestQuestion(
                id = "q9",
                questionText = "She ___ to have been a famous actress in her youth.",
                options = listOf("said", "told", "is said", "is telling"),
                correctOptionIndex = 2,
                difficulty = QuestionDifficulty.HARD
            ),
            TestQuestion(
                id = "q10",
                questionText = "The synonym of 'ubiquitous' is:",
                options = listOf("rare", "omnipresent", "beautiful", "dangerous"),
                correctOptionIndex = 1,
                difficulty = QuestionDifficulty.HARD
            )
        )
    }
}
