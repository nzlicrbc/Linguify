package com.example.linguify.data.services

import android.util.Log
import com.example.linguify.data.remote.GeminiApiService
import com.example.linguify.data.remote.model.*
import com.example.linguify.model.*
import org.json.JSONObject
import retrofit2.HttpException
import java.util.regex.Pattern
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class QuestionGeneratorService @Inject constructor(
    private val geminiApiService: GeminiApiService
) {

    suspend fun generateReviewQuestions(word: Word): List<ReviewQuestionType> {
        val questionTypes = listOf("multiple_choice", "context_sentence", "definition")
        val selectedType = questionTypes.random()

        Log.d("QuestionGenerator", "Generating ${selectedType} question for word: ${word.text}")

        val question = when (selectedType) {
            "multiple_choice" -> generateMultipleChoiceQuestion(word)
            "context_sentence" -> generateContextSentenceQuestion(word)
            "definition"       -> generateDefinitionQuestion(word)
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
                    "multiple_choice" -> generateMultipleChoiceQuestion(word)
                    "context_sentence" -> generateContextSentenceQuestion(word)
                    "definition" -> generateDefinitionQuestion(word)
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

    private suspend fun generateMultipleChoiceQuestion(word: Word): ReviewQuestionType.MultipleChoice? {
        val maxRetries = 2

        for (attempt in 1..maxRetries) {
            try {
                val prompt = createMultipleChoicePrompt(word)
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

    private suspend fun generateContextSentenceQuestion(word: Word): ReviewQuestionType.ContextSentence? {
        val maxRetries = 2

        for (attempt in 1..maxRetries) {
            try {
                val prompt = createContextSentencePrompt(word)
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

    private suspend fun generateDefinitionQuestion(word: Word): ReviewQuestionType.Definition? {
        val maxRetries = 2

        for (attempt in 1..maxRetries) {
            try {
                val prompt = createDefinitionPrompt(word)
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

    private fun createMultipleChoicePrompt(word: Word): String {
        val correctIndex = (0..3).random()
        val eWord = word.text.escapeJson()
        val eTranslation = (word.translation ?: "unknown").escapeJson()

        return """
Bir İngilizce kelime için Türkçe çeviri sorusu oluştur.

Kelime: "$eWord"
Doğru Türkçe çeviri: "$eTranslation"
Doğru cevap index $correctIndex konumunda olmalı.

Görev:
- options[$correctIndex] tam olarak "$eTranslation" olmalı
- Diğer 3 konum için farklı, gerçekçi ama yanlış Türkçe kelimeler üret
- Boş string veya placeholder kullanma

Sadece şu JSON formatında cevap ver (markdown yok, açıklama yok):
{"questionText": "\"$eWord\" kelimesinin Türkçe karşılığı nedir?", "options": ["seçenek0","seçenek1","seçenek2","seçenek3"], "correctIndex": $correctIndex}
""".trimIndent()
    }

    private fun createContextSentencePrompt(word: Word): String {
        val correctIndex = (0..3).random()
        val eWord = word.text.escapeJson()
        val wordType = (word.wordType ?: "word").escapeJson()

        return """
İngilizce "$eWord" ($wordType) kelimesi için boşluk doldurma sorusu oluştur.

Doğru cevap "$eWord" — options[$correctIndex] konumunda olmalı.
Diğer 3 konum için aynı sözcük türünden farklı, gerçekçi İngilizce kelimeler üret.
Cümle içinde boşluk için tam olarak ____ (4 alt çizgi) kullan.

Sadece şu JSON formatında cevap ver (markdown yok, açıklama yok):
{"sentence": "Tam olarak ____ bir boşluk içeren cümle.", "options": ["opt0","opt1","opt2","opt3"], "correctIndex": $correctIndex}

Kurallar:
- options[$correctIndex] tam olarak "$eWord" olmalı
- Boşluk tam olarak ____ olmalı
- Diğer 3 seçenek, boşluğa "$eWord" kadar iyi uymayan gerçek İngilizce kelimeler olmalı
- Boş string veya placeholder kullanma
""".trimIndent()
    }

    private fun createDefinitionPrompt(word: Word): String {
        val correctIndex = (0..3).random()
        val eWord = word.text.escapeJson()
        val correctDefinition = if (!word.definition.isNullOrBlank()) {
            word.definition!!
        } else {
            "a word meaning ${word.translation}"
        }
        val eCorrectDef = correctDefinition.escapeJson()

        return """
İngilizce "$eWord" kelimesi için tanım eşleştirme sorusu oluştur.

Doğru tanım: "$eCorrectDef" — definitions[$correctIndex] konumunda olmalı.
Diğer 3 konum için "$eWord" kelimesine uymayan, ama gerçekçi görünen İngilizce tanımlar üret.

Sadece şu JSON formatında cevap ver (markdown yok, açıklama yok):
{"definitions": ["tanım0","tanım1","tanım2","tanım3"], "correctIndex": $correctIndex}

Kurallar:
- definitions[$correctIndex] tam olarak "$eCorrectDef" olmalı
- Diğer 3 tanım gerçekçi ama "$eWord" için yanlış olmalı
- "Alternative definition", "Another definition" gibi placeholder kullanma
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
            val responseText = response.candidates?.firstOrNull()
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