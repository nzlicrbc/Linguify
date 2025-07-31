package com.example.linguify.utils

import android.content.Context
import android.util.Log
import com.example.linguify.model.CsvWordData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader

class CsvWordLoader(private val context: Context) {

    suspend fun loadWordsFromCsv(filename: String): List<CsvWordData> = withContext(Dispatchers.IO) {
        val words = mutableListOf<CsvWordData>()

        try {
            val inputStream = context.assets.open(filename)
            val reader = BufferedReader(InputStreamReader(inputStream))

            val headerLine = reader.readLine()
            if (headerLine == null) {
                Log.e("CsvWordLoader", "CSV file is empty or header is missing")
                return@withContext emptyList()
            }

            Log.d("CsvWordLoader", "CSV Header: $headerLine")

            val separator = if (headerLine.contains(",")) "," else "\t"
            val headers = headerLine.split(separator)

            Log.d("CsvWordLoader", "Headers found: ${headers.joinToString(", ")}")

            val wordIndex = headers.indexOf("headword")
            val typeIndex = headers.indexOf("pos")
            val levelIndex = headers.indexOf("CEFR")
            val translationIndex = headers.indexOf("Translations")

            if (wordIndex == -1 || levelIndex == -1 || translationIndex == -1) {
                Log.e("CsvWordLoader", "Required columns not found in CSV. wordIndex: $wordIndex, levelIndex: $levelIndex, translationIndex: $translationIndex")
                Log.e("CsvWordLoader", "Available headers: ${headers.joinToString(", ")}")
                return@withContext emptyList()
            }

            var line: String?
            var lineCount = 0
            var successCount = 0
            var errorCount = 0

            while (reader.readLine().also { line = it } != null) {
                lineCount++
                try {
                    val columns = parseCsvLine(line!!, separator)

                    if (columns.size > translationIndex) {
                        val word = columns[wordIndex].trim()
                        val wordType = if (typeIndex >= 0 && typeIndex < columns.size) columns[typeIndex].trim() else ""
                        val cefrLevel = columns[levelIndex].trim()
                        val translation = columns[translationIndex].trim()

                        if (word.isNotEmpty() && cefrLevel.isNotEmpty()) {
                            words.add(
                                CsvWordData(
                                    word = word,
                                    wordType = wordType,
                                    cefrLevel = cefrLevel,
                                    translation = translation
                                )
                            )
                            successCount++

                            if (successCount % 1000 == 0) {
                                Log.d("CsvWordLoader", "Processed $successCount words so far")
                            }
                        } else {
                            Log.w("CsvWordLoader", "Skipping line $lineCount: Empty word or CEFR level")
                            errorCount++
                        }
                    } else {
                        Log.w("CsvWordLoader", "Skipping line $lineCount: Too few columns - ${columns.size} < ${translationIndex + 1}")
                        errorCount++
                    }
                } catch (e: Exception) {
                    Log.e("CsvWordLoader", "Error parsing line $lineCount: ${e.message}", e)
                    errorCount++
                }
            }

            reader.close()
            inputStream.close()

            val countsByLevel = words.groupBy { it.cefrLevel }
                .mapValues { it.value.size }

            Log.d("CsvWordLoader", "Words by CEFR level: ${countsByLevel.entries.joinToString { "${it.key}: ${it.value}" }}")
            Log.d("CsvWordLoader", "Loaded ${words.size} words, skipped $errorCount lines with errors from total $lineCount lines")

            val beginnerExample = words.find { it.cefrLevel in listOf("A1", "A2") }
            val intermediateExample = words.find { it.cefrLevel in listOf("B1", "B2") }
            val advancedExample = words.find { it.cefrLevel in listOf("C1", "C2") }

            Log.d("CsvWordLoader", "Sample beginner word: $beginnerExample")
            Log.d("CsvWordLoader", "Sample intermediate word: $intermediateExample")
            Log.d("CsvWordLoader", "Sample advanced word: $advancedExample")

        } catch (e: Exception) {
            Log.e("CsvWordLoader", "Error loading CSV: ${e.message}", e)
        }

        return@withContext words
    }

    private fun parseCsvLine(line: String, separator: String = ","): List<String> {
        val result = mutableListOf<String>()
        var currentField = StringBuilder()
        var inQuotes = false

        for (char in line) {
            when {
                char == '"' -> inQuotes = !inQuotes
                char.toString() == separator && !inQuotes -> {
                    result.add(currentField.toString().trim('"', ' '))
                    currentField = StringBuilder()
                }
                else -> currentField.append(char)
            }
        }

        result.add(currentField.toString().trim('"', ' '))
        return result
    }
}
