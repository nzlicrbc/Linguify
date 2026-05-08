package com.example.linguify.data.services

import android.util.Log
import com.example.linguify.data.remote.GeminiApiService
import com.example.linguify.data.remote.model.*
import com.example.linguify.data.repositories.WordRepository
import com.example.linguify.model.*
import org.json.JSONObject
import retrofit2.HttpException
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestionGeneratorService @Inject constructor(
    private val geminiApiService: GeminiApiService,
    private val wordRepository: WordRepository
) {

    suspend fun generateReviewQuestions(word: Word): List<ReviewQuestionType> {
        val questionTypes = listOf("multiple_choice", "context_sentence", "definition")
        val selectedType = questionTypes.random()

        Log.d("QuestionGenerator", "Generating ${selectedType} question for word: ${word.text}")

        val question = when (selectedType) {
            "multiple_choice" -> generateMultipleChoiceQuestion(word)
            "context_sentence" -> generateContextSentenceQuestion(word)
            "definition" -> generateDefinitionQuestion(word)
            else -> null
        }

        return if (question != null) {
            listOf(question)
        } else {
            Log.e("QuestionGenerator", "Failed to generate question for ${word.text}")
            emptyList()
        }
    }

    suspend fun generateMixedReviewQuestions(words: List<Word>): List<ReviewQuestionType> {
        val questions = mutableListOf<ReviewQuestionType>()
        val allWords = getAllWordsForDistractors()

        Log.d("QuestionGenerator", "Generating mixed questions for ${words.size} words")

        for ((index, word) in words.withIndex()) {
            try {
                if (index > 0) {
                    kotlinx.coroutines.delay(2000)
                }

                val questionTypes = listOf("multiple_choice", "context_sentence", "definition")
                val selectedType = questionTypes.random()

                Log.d("QuestionGenerator", "Generating ${selectedType} for ${word.text} (${index + 1}/${words.size})")

                val question = when (selectedType) {
                    "multiple_choice" -> generateMultipleChoiceQuestion(word, allWords)
                    "context_sentence" -> generateContextSentenceQuestion(word, allWords)
                    "definition" -> generateDefinitionQuestion(word, allWords)
                    else -> null
                }

                if (question != null) {
                    questions.add(question)
                    Log.d("QuestionGenerator", "Generated ${selectedType} question for ${word.text}")
                } else {
                    Log.w("QuestionGenerator", "Failed to generate question for ${word.text}")
                }

            } catch (e: Exception) {
                Log.e("QuestionGenerator", "Error processing word ${word.text}: ${e.message}")
            }
        }

        Log.d("QuestionGenerator", "Final result: ${questions.size}/${words.size} questions generated")
        return questions.shuffled()
    }

    private suspend fun getAllWordsForDistractors(): List<Word> {
        return try {
            wordRepository.getSavedWordsFromFirebase()
        } catch (e: Exception) {
            Log.e("QuestionGenerator", "Error loading words for distractors: ${e.message}")
            emptyList()
        }
    }

    private suspend fun generateMultipleChoiceQuestion(word: Word, allWords: List<Word> = emptyList()): ReviewQuestionType.MultipleChoice? {
        val maxRetries = 2

        for (attempt in 1..maxRetries) {
            try {
                val prompt = createMultipleChoicePrompt(word, allWords)
                val response = callGeminiAPI(prompt)

                if (response != null) {
                    val question = parseMultipleChoiceResponse(response, word)
                    if (question != null) {
                        Log.d("QuestionGenerator", "MC question for ${word.text} on attempt $attempt")
                        return question
                    }
                }

                if (attempt < maxRetries) {
                    kotlinx.coroutines.delay(3000)
                }

            } catch (e: Exception) {
                Log.e("QuestionGenerator", "MC attempt $attempt failed for ${word.text}: ${e.message}")
                if (attempt < maxRetries) {
                    kotlinx.coroutines.delay(5000)
                }
            }
        }

        Log.e("QuestionGenerator", "Failed to generate MC question for ${word.text} after $maxRetries attempts")
        return null
    }

    private suspend fun generateContextSentenceQuestion(word: Word, allWords: List<Word> = emptyList()): ReviewQuestionType.ContextSentence? {
        val maxRetries = 2

        for (attempt in 1..maxRetries) {
            try {
                val prompt = createContextSentencePrompt(word, allWords)
                val response = callGeminiAPI(prompt)

                if (response != null) {
                    val question = parseContextSentenceResponse(response, word)
                    if (question != null) {
                        Log.d("QuestionGenerator", "CS question for ${word.text} on attempt $attempt")
                        return question
                    }
                }

                if (attempt < maxRetries) {
                    kotlinx.coroutines.delay(3000)
                }

            } catch (e: Exception) {
                Log.e("QuestionGenerator", "CS attempt $attempt failed for ${word.text}: ${e.message}")
                if (attempt < maxRetries) {
                    kotlinx.coroutines.delay(5000)
                }
            }
        }

        Log.e("QuestionGenerator", "Failed to generate CS question for ${word.text} after $maxRetries attempts")
        return null
    }

    private suspend fun generateDefinitionQuestion(word: Word, allWords: List<Word> = emptyList()): ReviewQuestionType.Definition? {
        val maxRetries = 2

        for (attempt in 1..maxRetries) {
            try {
                val prompt = createDefinitionPrompt(word, allWords)
                val response = callGeminiAPI(prompt)

                if (response != null) {
                    val question = parseDefinitionResponse(response, word)
                    if (question != null) {
                        Log.d("QuestionGenerator", "DEF question for ${word.text} on attempt $attempt")
                        return question
                    }
                }

                if (attempt < maxRetries) {
                    kotlinx.coroutines.delay(3000)
                }

            } catch (e: Exception) {
                Log.e("QuestionGenerator", "DEF attempt $attempt failed for ${word.text}: ${e.message}")
                if (attempt < maxRetries) {
                    kotlinx.coroutines.delay(5000)
                }
            }
        }

        Log.e("QuestionGenerator", "Failed to generate DEF question for ${word.text} after $maxRetries attempts")
        return null
    }

    private fun createMultipleChoicePrompt(word: Word, allWords: List<Word>): String {
        val randomCorrectIndex = (0..3).random()

        val wrongOptions = if (allWords.isNotEmpty()) {
            allWords.filter { it.text != word.text && !it.translation.isNullOrBlank() }
                .shuffled()
                .take(3)
                .map { it.translation!! }
        } else {
            listOf("wrong translate 1", "wrong translate 2", "wrong translate 3")
        }

        val options = mutableListOf<String>()
        var wrongOptionIndex = 0

        for (i in 0..3) {
            if (i == randomCorrectIndex) {
                options.add(word.translation ?: "unknown")
            } else {
                if (wrongOptionIndex < wrongOptions.size) {
                    options.add(wrongOptions[wrongOptionIndex])
                    wrongOptionIndex++
                } else {
                    options.add("${wrongOptionIndex}")
                    wrongOptionIndex++
                }
            }
        }

        val e0 = options[0].escapeJson()
        val e1 = options[1].escapeJson()
        val e2 = options[2].escapeJson()
        val e3 = options[3].escapeJson()
        val eWord = word.text.escapeJson()
        val eTranslation = (word.translation ?: "unknown").escapeJson()

        return """
Create a multiple choice question for the English word "$eWord".

Word: $eWord
Correct Translation: $eTranslation

CRITICAL INSTRUCTION: 
- The correct answer "$eTranslation" MUST be at position $randomCorrectIndex (index $randomCorrectIndex)
- Put these options in this EXACT order:
  Position 0: "$e0"
  Position 1: "$e1" 
  Position 2: "$e2"
  Position 3: "$e3"

Requirements:
- Ask "What does '$eWord' mean in Turkish?"
- Use EXACTLY these 4 options in the exact order shown above
- The correctIndex MUST be $randomCorrectIndex

Respond ONLY with this EXACT JSON (no changes to options order):
{"questionText": "What does '$eWord' mean in Turkish?", "options": ["$e0", "$e1", "$e2", "$e3"], "correctIndex": $randomCorrectIndex}

No markdown, no explanations, just the JSON above.
""".trimIndent()
    }

    private fun createContextSentencePrompt(word: Word, allWords: List<Word>): String {
        val randomCorrectIndex = (0..3).random()

        val sameTypeWords = if (allWords.isNotEmpty()) {
            allWords.filter { it.text != word.text && it.wordType == word.wordType }
                .shuffled()
                .take(3)
                .map { it.text }
        } else {
            listOf("wrong1", "wrong2", "wrong3")
        }

        val options = mutableListOf<String>()
        var wrongOptionIndex = 0

        for (i in 0..3) {
            if (i == randomCorrectIndex) {
                options.add(word.text)
            } else {
                if (wrongOptionIndex < sameTypeWords.size) {
                    options.add(sameTypeWords[wrongOptionIndex])
                    wrongOptionIndex++
                } else {
                    options.add("alternative${wrongOptionIndex}")
                    wrongOptionIndex++
                }
            }
        }

        val e0 = options[0].escapeJson()
        val e1 = options[1].escapeJson()
        val e2 = options[2].escapeJson()
        val e3 = options[3].escapeJson()
        val eWord = word.text.escapeJson()

        return """
Create a fill-in-the-blank sentence for the English word "$eWord".

Word: $eWord
Type: ${word.wordType ?: "unknown"}

CRITICAL INSTRUCTION:
- The correct answer "$eWord" MUST be at position $randomCorrectIndex (index $randomCorrectIndex)
- Put these options in this EXACT order:
  Position 0: "$e0"
  Position 1: "$e1"
  Position 2: "$e2"
  Position 3: "$e3"

Requirements:
- Create a natural English sentence with EXACTLY ONE blank: ____
- Use only 4 underscores (____) for the blank, no more, no less
- The sentence should clearly show where "$eWord" belongs
- Use EXACTLY these 4 options in the exact order shown above
- The correctIndex MUST be $randomCorrectIndex

Example format: "She decided to ____ the meeting until next week."

Respond ONLY with this EXACT JSON (no changes to options order):
{"sentence": "A sentence with exactly ____ one blank.", "options": ["$e0", "$e1", "$e2", "$e3"], "correctIndex": $randomCorrectIndex}

No markdown, no explanations, just the JSON above.
""".trimIndent()
    }

    private fun createDefinitionPrompt(word: Word, allWords: List<Word>): String {
        val randomCorrectIndex = (0..3).random()

        val correctDefinition = if (!word.definition.isNullOrBlank()) {
            word.definition!!
        } else {
            "A word meaning ${word.translation}"
        }

        val otherDefinitions = if (allWords.isNotEmpty()) {
            allWords.filter { it.text != word.text && !it.definition.isNullOrBlank() }
                .shuffled()
                .take(3)
                .map { it.definition!! }
        } else {
            listOf("Another definition", "Different meaning", "Alternative explanation")
        }

        val definitions = mutableListOf<String>()
        var wrongDefinitionIndex = 0

        for (i in 0..3) {
            if (i == randomCorrectIndex) {
                definitions.add(correctDefinition)
            } else {
                if (wrongDefinitionIndex < otherDefinitions.size) {
                    definitions.add(otherDefinitions[wrongDefinitionIndex])
                    wrongDefinitionIndex++
                } else {
                    definitions.add("Alternative definition ${wrongDefinitionIndex}")
                    wrongDefinitionIndex++
                }
            }
        }

        val e0 = definitions[0].escapeJson()
        val e1 = definitions[1].escapeJson()
        val e2 = definitions[2].escapeJson()
        val e3 = definitions[3].escapeJson()
        val eWord = word.text.escapeJson()
        val eCorrectDef = correctDefinition.escapeJson()

        return """
Create a definition matching question for the English word "$eWord".

Word: $eWord
Correct Definition: $eCorrectDef

CRITICAL INSTRUCTION:
- The correct definition "$eCorrectDef" MUST be at position $randomCorrectIndex (index $randomCorrectIndex)
- Put these definitions in this EXACT order:
  Position 0: "$e0"
  Position 1: "$e1"
  Position 2: "$e2"
  Position 3: "$e3"

Requirements:
- Ask "Which definition matches '$eWord'?"
- Use EXACTLY these 4 definitions in the exact order shown above
- The correctIndex MUST be $randomCorrectIndex

Respond ONLY with this EXACT JSON (no changes to definitions order):
{"definitions": ["$e0", "$e1", "$e2", "$e3"], "correctIndex": $randomCorrectIndex}

No markdown, no explanations, just the JSON above.
""".trimIndent()
    }

    private suspend fun callGeminiAPI(prompt: String): String? {
        return try {
            kotlinx.coroutines.delay(1000)

            val request = GeminiRequest(
                contents = listOf(
                    Content(
                        parts = listOf(Part(text = prompt))
                    )
                )
            )

            val response = geminiApiService.generateContent(request = request)
            val responseText = response.candidates.firstOrNull()
                ?.content?.parts?.firstOrNull()?.text

            responseText?.let { cleanJsonResponse(it) }
        } catch (e: Exception) {
            when {
                e is HttpException && e.code() == 429 -> {
                    Log.w("QuestionGenerator", "Rate limit hit, waiting longer...")
                    kotlinx.coroutines.delay(5000)
                    null
                }
                e.message?.contains("quota", ignoreCase = true) == true -> {
                    Log.e("QuestionGenerator", "API quota exceeded")
                    null
                }
                else -> {
                    Log.e("QuestionGenerator", "Gemini API call failed: ${e.message}")
                    null
                }
            }
        }
    }

    private fun String.escapeJson(): String = replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")

    private fun cleanJsonResponse(response: String): String {
        val markdownPattern = Pattern.compile("```(?:json)?\\s*(.+?)\\s*```", Pattern.DOTALL)
        val markdownMatcher = markdownPattern.matcher(response)
        if (markdownMatcher.find()) {
            return markdownMatcher.group(1)?.trim() ?: response.trim()
        }

        val jsonPattern = Pattern.compile("\\{.+\\}", Pattern.DOTALL)
        val jsonMatcher = jsonPattern.matcher(response)
        return if (jsonMatcher.find()) {
            jsonMatcher.group()?.trim() ?: response.trim()
        } else {
            response.trim()
        }
    }

    private fun parseMultipleChoiceResponse(response: String, word: Word): ReviewQuestionType.MultipleChoice? {
        return try {
            val json = JSONObject(response)
            val questionText = json.getString("questionText")
            val correctIndex = json.getInt("correctIndex")

            val optionsArray = json.getJSONArray("options")
            val options = mutableListOf<String>()
            for (i in 0 until optionsArray.length()) {
                options.add(optionsArray.getString(i))
            }

            if (options.size == 4 && correctIndex in 0..3) {
                if (options[correctIndex].trim() == word.translation?.trim()) {
                    ReviewQuestionType.MultipleChoice(
                        word = word,
                        questionText = questionText,
                        options = options,
                        correctIndex = correctIndex
                    )
                } else {
                    Log.e("Parser", "Correct answer not at specified index for ${word.text}")
                    null
                }
            } else {
                Log.e("Parser", "Invalid MC options size or correctIndex for ${word.text}")
                null
            }
        } catch (e: Exception) {
            Log.e("Parser", "Error parsing MC response for ${word.text}: ${e.message}")
            null
        }
    }

    private fun parseContextSentenceResponse(response: String, word: Word): ReviewQuestionType.ContextSentence? {
        return try {
            val json = JSONObject(response)
            var sentence = json.getString("sentence")
            val correctIndex = json.getInt("correctIndex")

            val optionsArray = json.getJSONArray("options")
            val options = mutableListOf<String>()
            for (i in 0 until optionsArray.length()) {
                options.add(optionsArray.getString(i))
            }

            sentence = sentence.replace(Regex("_{3,}"), "____")
            sentence = sentence.replace(Regex("_+\\s+_+"), "____")

            if (!sentence.contains("____")) {
                sentence = "$sentence ____"
            }

            if (options.size == 4 && correctIndex in 0..3 && sentence.contains("____")) {
                if (options[correctIndex].trim() == word.text.trim()) {
                    ReviewQuestionType.ContextSentence(
                        word = word,
                        sentence = sentence,
                        options = options,
                        correctIndex = correctIndex
                    )
                } else {
                    Log.e("Parser", "Correct answer not at specified index for ${word.text}")
                    null
                }
            } else {
                Log.e("Parser", "Invalid CS format for ${word.text} - sentence: $sentence")
                null
            }
        } catch (e: Exception) {
            Log.e("Parser", "Error parsing CS response for ${word.text}: ${e.message}")
            null
        }
    }

    private fun parseDefinitionResponse(response: String, word: Word): ReviewQuestionType.Definition? {
        return try {
            val json = JSONObject(response)
            val correctIndex = json.getInt("correctIndex")

            val definitionsArray = json.getJSONArray("definitions")
            val definitions = mutableListOf<String>()
            for (i in 0 until definitionsArray.length()) {
                definitions.add(definitionsArray.getString(i))
            }

            if (definitions.size == 4 && correctIndex in 0..3) {
                val expectedDefinition = if (!word.definition.isNullOrBlank()) {
                    word.definition!!
                } else {
                    "A word meaning ${word.translation}"
                }

                if (definitions[correctIndex].contains(expectedDefinition) ||
                    expectedDefinition.contains(definitions[correctIndex])) {
                    ReviewQuestionType.Definition(
                        word = word,
                        definitions = definitions,
                        correctIndex = correctIndex
                    )
                } else {
                    Log.e("Parser", "Correct definition not at specified index for ${word.text}")
                    null
                }
            } else {
                Log.e("Parser", "Invalid DEF options size or correctIndex for ${word.text}")
                null
            }
        } catch (e: Exception) {
            Log.e("Parser", "Error parsing DEF response for ${word.text}: ${e.message}")
            null
        }
    }
}