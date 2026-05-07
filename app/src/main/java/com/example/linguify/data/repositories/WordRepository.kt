package com.example.linguify.data.repositories

import android.content.SharedPreferences
import android.util.Log
import com.example.linguify.BuildConfig
import com.example.linguify.data.local.WordDao
import com.example.linguify.data.remote.WordsApiService
import com.example.linguify.model.Word
import com.example.linguify.model.WordDetail
import com.example.linguify.model.WordEntity
import com.example.linguify.model.WordLearningStatus
import com.example.linguify.utils.CsvWordLoader
import com.example.linguify.utils.UserLevel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WordRepository @Inject constructor(
    private val wordDao: WordDao,
    private val csvWordLoader: CsvWordLoader,
    private val firestore: FirebaseFirestore,
    private val firebaseAuth: FirebaseAuth,
    private val sharedPreferences: SharedPreferences
) {
    companion object {
        private const val COLLECTION_USER_WORDS = "user_words"
        private const val KEY_LAST_WORD_INDEX_PREFIX = "last_word_index_"
        private const val WORDS_PER_SET = 20
        private const val CSV_FILENAME = "words_dataset.csv"
    }

    private fun getUserSpecificKey(key: String): String {
        val userId = firebaseAuth.currentUser?.uid ?: "default"
        return "${key}_$userId"
    }

    private fun getCurrentUserId(): String {
        return firebaseAuth.currentUser?.uid ?: "default_user"
    }

    @Inject
    lateinit var wordsApiService: WordsApiService

    private val wordCache = mutableMapOf<String, Word>()
    private val wordSetCache = mutableMapOf<String, List<Word>>()
    private val userWordStatusCache = mutableMapOf<String, WordLearningStatus>()
    private var cachedWordCounts: WordCounts? = null

    data class WordCounts(
        val totalWords: Int = 0,
        val knownWords: Int = 0,
        val toLearnWords: Int = 0,
        val learningWords: Int = 0,
        val reviewWords: Int = 0
    )

    suspend fun checkAndReloadDatabase(): Boolean = withContext(Dispatchers.IO) {
        val isInitialized = sharedPreferences.getBoolean("is_db_initialized", false)
        var hasData = false

        try {
            val wordCount = wordDao.getWordCountByCefrLevels(listOf("A1", "A2", "B1", "B2", "C1", "C2"))

            Log.d("WordRepository", "Total words in database: $wordCount")

            val beginnerCount = wordDao.getWordCountByCefrLevels(listOf("A1", "A2"))
            val intermediateCount = wordDao.getWordCountByCefrLevels(listOf("B1", "B2"))
            val advancedCount = wordDao.getWordCountByCefrLevels(listOf("C1", "C2"))

            Log.d("WordRepository", "Beginner words: $beginnerCount")
            Log.d("WordRepository", "Intermediate words: $intermediateCount")
            Log.d("WordRepository", "Advanced words: $advancedCount")

            hasData = wordCount > 0

            if (isInitialized && !hasData) {
                Log.d("WordRepository", "Database is marked as initialized but has no data. Forcing reload.")
                sharedPreferences.edit().putBoolean("is_db_initialized", false).apply()
                return@withContext initializeDatabase()
            }

            if (!isInitialized) {
                return@withContext initializeDatabase()
            }

            return@withContext hasData
        } catch (e: Exception) {
            Log.e("WordRepository", "Error checking database status: ${e.message}", e)

            if (!isInitialized || !hasData) {
                return@withContext initializeDatabase()
            }

            return@withContext false
        }
    }

    suspend fun initializeDatabase(): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d("WordRepository", "Initializing database from CSV: $CSV_FILENAME")

            val csvWords = csvWordLoader.loadWordsFromCsv(CSV_FILENAME)
            Log.d("WordRepository", "Loaded ${csvWords.size} words from CSV")

            if (csvWords.isEmpty()) {
                Log.e("WordRepository", "No words loaded from CSV!")
                return@withContext false
            }

            val a1Word = csvWords.find { it.cefrLevel == "A1" }
            val b1Word = csvWords.find { it.cefrLevel == "B1" }
            val c1Word = csvWords.find { it.cefrLevel == "C1" }

            Log.d("WordRepository", "Sample A1 word: $a1Word")
            Log.d("WordRepository", "Sample B1 word: $b1Word")
            Log.d("WordRepository", "Sample C1 word: $c1Word")

            val wordEntities = csvWords.map { csvWord ->
                WordEntity(
                    id = "${csvWord.word}_${csvWord.cefrLevel}".hashCode().toString(),
                    text = csvWord.word,
                    wordType = csvWord.wordType,
                    cefrLevel = csvWord.cefrLevel,
                    translation = csvWord.translation,
                    definition = null,
                    example = null,
                    pronunciationUrl = null
                )
            }

            wordDao.deleteAllWords()

            wordDao.insertWords(wordEntities)

            sharedPreferences.edit().putBoolean("is_db_initialized", true).apply()

            Log.d("WordRepository", "Database initialized with ${wordEntities.size} words")

            val wordCount = wordDao.getWordCountByCefrLevels(listOf("A1", "A2", "B1", "B2", "C1", "C2"))
            val beginnerCount = wordDao.getWordCountByCefrLevels(listOf("A1", "A2"))
            val intermediateCount = wordDao.getWordCountByCefrLevels(listOf("B1", "B2"))
            val advancedCount = wordDao.getWordCountByCefrLevels(listOf("C1", "C2"))

            Log.d("WordRepository", "Verification - Total words: $wordCount")
            Log.d("WordRepository", "Verification - Beginner: $beginnerCount, Intermediate: $intermediateCount, Advanced: $advancedCount")

            return@withContext wordCount > 0
        } catch (e: Exception) {
            Log.e("WordRepository", "Error initializing database: ${e.message}", e)
            return@withContext false
        }
    }

    suspend fun deleteAllWords() = withContext(Dispatchers.IO) {
        wordDao.deleteAllWords()
    }

    suspend fun getWordsForLevel(level: UserLevel): List<Word> = withContext(Dispatchers.IO) {
        val userId = getCurrentUserId()
        val cacheKey = "user_${userId}_level_${level.code}"

        wordSetCache[cacheKey]?.let { cachedWords ->
            Log.d("WordRepository", "Loaded ${cachedWords.size} cached words for user: $userId, level: ${level.code}")
            return@withContext cachedWords
        }

        try {
            Log.d("WordRepository", "Fetching NEW words for user: $userId, level: ${level.code}")

            val cefrLevels = level.cefrLevels
            Log.d("WordRepository", "CEFR levels for ${level.code}: $cefrLevels")

            var wordCount = 0
            try {
                wordCount = wordDao.getWordCountByCefrLevels(cefrLevels)
                Log.d("WordRepository", "Found $wordCount words for CEFR levels: $cefrLevels")
            } catch (e: Exception) {
                Log.e("WordRepository", "Error getting word count: ${e.message}")
            }

            if (wordCount == 0) {
                val initialized = checkAndReloadDatabase()
                if (!initialized) {
                    Log.e("WordRepository", "Failed to initialize database")
                    return@withContext emptyList()
                }
            }

            val knownAndToLearnIds = getKnownWordIds() + getToLearnWordIds() + getLearningWordIds()
            Log.d("WordRepository", "Excluding ${knownAndToLearnIds.size} already processed words for user: $userId")

            var wordEntities = if (knownAndToLearnIds.isEmpty()) {
                wordDao.getRandomWordsByCefrLevels(cefrLevels, WORDS_PER_SET)
            } else {
                try {
                    wordDao.getRandomWordsByCefrLevelsExcludingIds(cefrLevels, WORDS_PER_SET, knownAndToLearnIds)
                } catch (e: Exception) {
                    Log.e("WordRepository", "Error fetching words excluding IDs: ${e.message}")
                    wordDao.getRandomWordsByCefrLevels(cefrLevels, WORDS_PER_SET)
                }
            }

            Log.d("WordRepository", "Found ${wordEntities.size} NEW words for user: $userId, levels: $cefrLevels")

            if (wordEntities.size < WORDS_PER_SET / 2) {
                Log.d("WordRepository", "Not enough unseen words, getting all words for the level")
                wordEntities = wordDao.getRandomWordsByCefrLevels(cefrLevels, WORDS_PER_SET)
                Log.d("WordRepository", "After retry: ${wordEntities.size} words")
            }

            val words = fetchWordDetailsAndMap(wordEntities)

            wordSetCache[cacheKey] = words

            Log.d("WordRepository", "Successfully cached ${words.size} words for user: $userId")

            return@withContext words
        } catch (e: Exception) {
            Log.e("WordRepository", "Error fetching words: ${e.message}", e)
            return@withContext emptyList()
        }
    }

    fun clearUserCache(userId: String? = null) {
        val targetUserId = userId ?: getCurrentUserId()

        val keysToRemove = wordSetCache.keys.filter { it.startsWith("user_${targetUserId}_") }
        keysToRemove.forEach { wordSetCache.remove(it) }

        wordCache.clear()
        userWordStatusCache.clear()
        cachedWordCounts = null

        Log.d("WordRepository", "Cleared cache for user: $targetUserId (${keysToRemove.size} entries)")
    }

    fun onUserChanged(newUserId: String) {
        clearUserCache()
        Log.d("WordRepository", "User changed, cache cleared for: $newUserId")
    }

    private suspend fun fetchWordDetailsAndMap(entities: List<WordEntity>): List<Word> = withContext(Dispatchers.IO) {
        val words = mutableListOf<Word>()

        for (entity in entities) {
            try {
                val userLevel = when {
                    entity.cefrLevel in listOf("A1", "A2") -> "beginner"
                    entity.cefrLevel in listOf("B1", "B2") -> "intermediate"
                    else -> "advanced"
                }

                words.add(
                    Word(
                        id = entity.id,
                        text = entity.text,
                        translation = entity.translation,
                        definition = entity.definition,
                        example = entity.example,
                        level = userLevel,
                        pronunciationUrl = entity.pronunciationUrl,
                        synonyms = emptyList(),
                        antonyms = emptyList(),
                        wordType = entity.wordType,
                        status = getUserWordStatus(entity.id)
                    )
                )
            } catch (e: Exception) {
                Log.e("WordRepository", "Error processing word ${entity.text}: ${e.message}")
            }
        }

        return@withContext words
    }

    suspend fun updateWordStatus(wordId: String, status: WordLearningStatus) =
        withContext(Dispatchers.IO) {
            firebaseAuth.currentUser?.let { user ->
                val userWordRef = firestore.collection(COLLECTION_USER_WORDS)
                    .document(user.uid)
                    .collection("words")
                    .document(wordId)

                val wordData = hashMapOf(
                    "status" to status.name,
                    "lastUpdated" to System.currentTimeMillis()
                )

                userWordRef.set(wordData).await()

                Log.d("WordRepository", "Updated word $wordId status to $status")
            }
        }

    private suspend fun getUserWordStatus(wordId: String): WordLearningStatus = withContext(Dispatchers.IO) {
        userWordStatusCache[wordId]?.let {
            return@withContext it
        }

        firebaseAuth.currentUser?.let { user ->
            try {
                val document = firestore.collection(COLLECTION_USER_WORDS)
                    .document(user.uid)
                    .collection("words")
                    .document(wordId)
                    .get()
                    .await()

                if (document.exists()) {
                    val statusStr = document.getString("status")
                    statusStr?.let {
                        try {
                            val status = WordLearningStatus.valueOf(it)
                            userWordStatusCache[wordId] = status
                            return@withContext status
                        } catch (e: IllegalArgumentException) {
                            return@withContext WordLearningStatus.NEW
                        }
                    }
                }

                return@withContext WordLearningStatus.NEW
            } catch (e: Exception) {
                return@withContext WordLearningStatus.NEW
            }
        } ?: return@withContext WordLearningStatus.NEW
    }

    suspend fun saveLastWordIndex(levelCode: String, index: Int) = withContext(Dispatchers.IO) {
        sharedPreferences.edit()
            .putInt(getUserSpecificKey("${KEY_LAST_WORD_INDEX_PREFIX}${levelCode}"), index)
            .apply()

        Log.d("WordRepository", "Saved last word index for $levelCode: $index")

        firebaseAuth.currentUser?.let { user ->
            try {
                val userData = hashMapOf(
                    "lastWordIndex_${levelCode}" to index,
                    "lastUpdated" to System.currentTimeMillis()
                )

                firestore.collection("users")
                    .document(user.uid)
                    .update(userData as Map<String, Any>)
                    .await()
            } catch (e: Exception) {
                val userData = hashMapOf(
                    "lastWordIndex_${levelCode}" to index,
                    "lastUpdated" to System.currentTimeMillis(),
                    "userId" to user.uid
                )

                firestore.collection("users")
                    .document(user.uid)
                    .set(userData)
                    .await()
            }
        }
    }

    suspend fun getLastWordIndex(levelCode: String): Int = withContext(Dispatchers.IO) {
        val localIndex = sharedPreferences.getInt(
            getUserSpecificKey("${KEY_LAST_WORD_INDEX_PREFIX}${levelCode}"),
            0
        )

        if (localIndex > 0) {
            return@withContext localIndex
        }

        firebaseAuth.currentUser?.let { user ->
            try {
                val userDoc = firestore.collection("users")
                    .document(user.uid)
                    .get()
                    .await()

                if (userDoc.exists()) {
                    val index = userDoc.getLong("lastWordIndex_${levelCode}")?.toInt() ?: 0

                    sharedPreferences.edit()
                        .putInt(getUserSpecificKey("${KEY_LAST_WORD_INDEX_PREFIX}${levelCode}"), index)
                        .apply()

                    return@withContext index
                }
            } catch (e: Exception) {
                Log.e("WordRepository", "Error getting last word index: ${e.message}")
                return@withContext 0
            }
        }

        return@withContext 0
    }

    suspend fun getKnownWords(levelCode: String): List<Word> = withContext(Dispatchers.IO) {
        val knownWordsIds = getKnownWordIds()

        if (knownWordsIds.isEmpty()) {
            return@withContext emptyList()
        }

        val wordEntities = wordDao.getWordsByIds(knownWordsIds)
        return@withContext fetchWordDetailsAndMap(wordEntities)
    }

    suspend fun getKnownWordIds(): List<String> = withContext(Dispatchers.IO) {
        firebaseAuth.currentUser?.let { user ->
            try {
                firestore.collection(COLLECTION_USER_WORDS)
                    .document(user.uid)
                    .collection("words")
                    .whereEqualTo("status", WordLearningStatus.KNOWN.name)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { it.id }
            } catch (e: Exception) {
                Log.e("WordRepository", "Error fetching known word IDs: ${e.message}")
                emptyList()
            }
        } ?: emptyList()
    }

    suspend fun getKnownWordsPaged(levelCode: String, limit: Int, offset: Int): List<Word> = withContext(Dispatchers.IO) {
        val knownWordsIds = getKnownWordIds()

        if (knownWordsIds.isEmpty()) {
            return@withContext emptyList()
        }

        val pageIds = knownWordsIds
            .drop(offset)
            .take(limit)

        if (pageIds.isEmpty()) {
            return@withContext emptyList()
        }

        val wordEntities = wordDao.getWordsByIds(pageIds)
        return@withContext fetchWordDetailsAndMap(wordEntities)
    }

    suspend fun getToLearnWordsPaged(levelCode: String, limit: Int, offset: Int): List<Word> = withContext(Dispatchers.IO) {
        val toLearnWordsIds = getToLearnWordIds()

        if (toLearnWordsIds.isEmpty()) {
            return@withContext emptyList()
        }

        val pageIds = toLearnWordsIds
            .drop(offset)
            .take(limit)

        if (pageIds.isEmpty()) {
            return@withContext emptyList()
        }

        val wordEntities = wordDao.getWordsByIds(pageIds)
        return@withContext fetchWordDetailsAndMap(wordEntities)
    }

    suspend fun getToLearnWords(levelCode: String): List<Word> = withContext(Dispatchers.IO) {
        val toLearnWordsIds = getToLearnWordIds()

        if (toLearnWordsIds.isEmpty()) {
            return@withContext emptyList()
        }

        val wordEntities = wordDao.getWordsByIds(toLearnWordsIds)
        return@withContext fetchWordDetailsAndMap(wordEntities)
    }

    suspend fun getToLearnWordIds(): List<String> = withContext(Dispatchers.IO) {
        firebaseAuth.currentUser?.let { user ->
            try {
                firestore.collection(COLLECTION_USER_WORDS)
                    .document(user.uid)
                    .collection("words")
                    .whereEqualTo("status", WordLearningStatus.TO_LEARN.name)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { it.id }
            } catch (e: Exception) {
                Log.e("WordRepository", "Error fetching to learn word IDs: ${e.message}")
                emptyList()
            }
        } ?: emptyList()
    }

    suspend fun saveWordToFirebase(word: Word) = withContext(Dispatchers.IO) {
        firebaseAuth.currentUser?.let { user ->
            try {
                val wordData = hashMapOf(
                    "id" to word.id,
                    "text" to word.text,
                    "translation" to word.translation,
                    "definition" to word.definition,
                    "example" to word.example,
                    "level" to word.level,
                    "pronunciationUrl" to word.pronunciationUrl,
                    "wordType" to word.wordType,
                    "status" to word.status.name,
                    "imageUrl" to word.imageUrl,
                    "lastUpdated" to System.currentTimeMillis()
                )

                firestore.collection("users")
                    .document(user.uid)
                    .collection("saved_words")
                    .document(word.id)
                    .set(wordData)
                    .await()

                Log.d("WordRepository", "Word saved to Firebase with image: ${word.text}")
            } catch (e: Exception) {
                Log.e("WordRepository", "Error saving word to Firebase: ${e.message}")
            }
        }
    }

    suspend fun getSavedWordsFromFirebase(): List<Word> = withContext(Dispatchers.IO) {
        val savedWords = mutableListOf<Word>()

        firebaseAuth.currentUser?.let { user ->
            try {
                val documents = firestore.collection("users")
                    .document(user.uid)
                    .collection("saved_words")
                    .get()
                    .await()
                    .documents

                for (document in documents) {
                    val word = Word(
                        id = document.getString("id") ?: "",
                        text = document.getString("text") ?: "",
                        translation = document.getString("translation") ?: "",
                        definition = document.getString("definition"),
                        example = document.getString("example"),
                        level = document.getString("level") ?: "",
                        pronunciationUrl = document.getString("pronunciationUrl"),
                        wordType = document.getString("wordType"),
                        synonyms = emptyList(),
                        antonyms = emptyList(),
                        status = try {
                            document.getString("status")?.let { WordLearningStatus.valueOf(it) }
                                ?: WordLearningStatus.NEW
                        } catch (e: IllegalArgumentException) {
                            WordLearningStatus.NEW
                        }
                    )
                    savedWords.add(word)
                }

                Log.d("WordRepository", "Loaded ${savedWords.size} saved words from Firebase")
            } catch (e: Exception) {
                Log.e("WordRepository", "Error loading saved words from Firebase: ${e.message}")
            }
        }

        return@withContext savedWords
    }

    fun listenToWordCounts(
        userLevel: UserLevel,
        onUpdate: (WordCounts) -> Unit,
        onError: (String) -> Unit
    ): ListenerRegistration? {
        val user = firebaseAuth.currentUser ?: return null
        val userId = user.uid

        cachedWordCounts?.let {
            onUpdate(it)
        }

        val userWordsRef = firestore.collection(COLLECTION_USER_WORDS)
            .document(userId)
            .collection("words")

        return userWordsRef.addSnapshotListener { snapshot, e ->
            if (e != null) {
                Log.e("WordRepository", "Error listening to word counts: ${e.message}")
                onError(e.message ?: "Firestore listener error")
                return@addSnapshotListener
            }

            if (snapshot != null) {
                try {
                    val allStatuses = snapshot.documents.mapNotNull {
                        it.getString("status")?.let { status ->
                            try {
                                WordLearningStatus.valueOf(status)
                            } catch (e: IllegalArgumentException) {
                                null
                            }
                        }
                    }

                    val knownWords = allStatuses.count { it == WordLearningStatus.KNOWN }
                    val toLearnWords = allStatuses.count { it == WordLearningStatus.TO_LEARN }
                    val learningWords = allStatuses.count { it == WordLearningStatus.LEARNING }

                    kotlinx.coroutines.runBlocking {
                        val totalWordCount = knownWords + toLearnWords + learningWords +
                                getWordCountByCefrLevels(userLevel.cefrLevels)

                        val reviewWords = (learningWords / 2).coerceAtMost(20)

                        val updatedCounts = WordCounts(
                            totalWords = totalWordCount,
                            knownWords = knownWords,
                            toLearnWords = toLearnWords,
                            learningWords = learningWords,
                            reviewWords = reviewWords
                        )

                        cachedWordCounts = updatedCounts

                        onUpdate(updatedCounts)
                    }
                } catch (e: Exception) {
                    Log.e("WordRepository", "Error processing word counts snapshot: ${e.message}")
                    onError("Kelime sayıları hesaplanırken hata: ${e.message}")
                }
            }
        }
    }

    suspend fun updateMultipleWordStatuses(wordStatuses: Map<String, WordLearningStatus>) = withContext(Dispatchers.IO) {
        firebaseAuth.currentUser?.let { user ->
            try {
                val batch = firestore.batch()

                wordStatuses.forEach { (wordId, status) ->
                    userWordStatusCache[wordId] = status

                    val userWordRef = firestore.collection(COLLECTION_USER_WORDS)
                        .document(user.uid)
                        .collection("words")
                        .document(wordId)

                    val wordData = hashMapOf(
                        "status" to status.name,
                        "lastUpdated" to System.currentTimeMillis()
                    )

                    batch.set(userWordRef, wordData)
                }

                batch.commit().await()
                Log.d("WordRepository", "Updated ${wordStatuses.size} word statuses in batch")
            } catch (e: Exception) {
                Log.e("WordRepository", "Error updating word statuses in batch: ${e.message}")
            }
        }
    }

    fun removeWordCountsListener(listener: Any) {
        if (listener is ListenerRegistration) {
            listener.remove()
        }
    }

    suspend fun checkDatabaseStatus() = withContext(Dispatchers.IO) {
        try {
            val wordCount = wordDao.getWordCountByCefrLevels(listOf("A1", "A2", "B1", "B2", "C1", "C2"))
            Log.d("WordRepository", "Total words in database: $wordCount")

            val beginnerCount = wordDao.getWordCountByCefrLevels(listOf("A1", "A2"))
            val intermediateCount = wordDao.getWordCountByCefrLevels(listOf("B1", "B2"))
            val advancedCount = wordDao.getWordCountByCefrLevels(listOf("C1", "C2"))

            Log.d("WordRepository", "Beginner words: $beginnerCount")
            Log.d("WordRepository", "Intermediate words: $intermediateCount")
            Log.d("WordRepository", "Advanced words: $advancedCount")

            return@withContext wordCount > 0
        } catch (e: Exception) {
            Log.e("WordRepository", "Error checking database status: ${e.message}", e)
            return@withContext false
        }
    }

    suspend fun getWordCountByCefrLevels(cefrLevels: List<String>): Int = withContext(Dispatchers.IO) {
        return@withContext wordDao.getWordCountByCefrLevels(cefrLevels)
    }

    suspend fun getWordsByIds(wordIds: List<String>): List<WordEntity> = withContext(Dispatchers.IO) {
        return@withContext if (wordIds.isEmpty()) {
            emptyList()
        } else {
            wordDao.getWordsByIds(wordIds)
        }
    }

    suspend fun updateWordDetails(
        wordId: String,
        definition: String? = null,
        example: String? = null,
        pronunciationUrl: String? = null
    ) = withContext(Dispatchers.IO) {
        try {
            wordDao.updateWordDetails(
                wordId = wordId,
                definition = definition,
                example = example,
                pronunciationUrl = pronunciationUrl,
                translation = null
            )
            Log.d("WordRepository", "Updated word details for ID: $wordId")
        } catch (e: Exception) {
            Log.e("WordRepository", "Error updating word details: ${e.message}")
        }
    }

    suspend fun fetchWordDetailsAndMapToWord(entities: List<WordEntity>): List<Word> = withContext(Dispatchers.IO) {
        return@withContext fetchWordDetailsAndMap(entities)
    }

    suspend fun getDiscoverableWordCount(userLevel: UserLevel): Int = withContext(Dispatchers.IO) {
        try {
            val totalWordsForLevel = wordDao.getWordCountByCefrLevels(userLevel.cefrLevels)

            val knownAndToLearnIds = getKnownWordIds() + getToLearnWordIds()

            val discoverableCount = totalWordsForLevel - knownAndToLearnIds.size

            return@withContext discoverableCount.coerceAtLeast(0)
        } catch (e: Exception) {
            Log.e("WordRepository", "Error calculating discoverable word count: ${e.message}")
            return@withContext 0
        }
    }

    suspend fun fetchWordDetailsFromApi(word: String): WordDetail? = withContext(Dispatchers.IO) {
        try {
            Log.d("WordRepository", "Fetching details from API for: $word")

            val apiKey = BuildConfig.WORDS_API_KEY
            val apiHost = "wordsapiv1.p.rapidapi.com"

            val detailsResponse = wordsApiService.getWordDetails(word, apiKey, apiHost)

            val synonymsResponse = wordsApiService.getSynonyms(word, apiKey, apiHost)

            val antonymsResponse = wordsApiService.getAntonyms(word, apiKey, apiHost)

            val examplesResponse = wordsApiService.getExamples(word, apiKey, apiHost)

            val definition = detailsResponse.results?.firstOrNull()?.definition
            val synonyms = synonymsResponse.synonyms ?: emptyList()
            val antonyms = antonymsResponse.antonyms ?: emptyList()
            val examples = examplesResponse.examples ?: emptyList()
            val pronunciation = detailsResponse.pronunciation?.all
            val wordType = detailsResponse.results?.firstOrNull()?.partOfSpeech

            val wordEntity = wordDao.getWordByText(word)

            return@withContext wordEntity?.let {
                WordDetail(
                    id = it.id,
                    text = word,
                    translation = it.translation,
                    definition = definition,
                    examples = examples,
                    synonyms = synonyms,
                    antonyms = antonyms,
                    phoneticSpelling = pronunciation,
                    pronunciationUrl = it.pronunciationUrl,
                    wordType = wordType ?: it.wordType,
                    level = when (it.cefrLevel) {
                        "A1", "A2" -> "beginner"
                        "B1", "B2" -> "intermediate"
                        "C1", "C2" -> "advanced"
                        else -> "intermediate"
                    },
                    status = getUserWordStatus(it.id)
                )
            }

        } catch (e: Exception) {
            Log.e("WordRepository", "Error fetching word details from API: ${e.message}", e)
            return@withContext null
        }
    }

    suspend fun fetchWordPronunciation(word: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.d("WordRepository", "Fetching pronunciation for: $word")

            val apiKey = BuildConfig.WORDS_API_KEY
            val apiHost = "wordsapiv1.p.rapidapi.com"

            val pronunciationResponse = wordsApiService.getPronunciation(word, apiKey, apiHost)

            val phonetic = pronunciationResponse.pronunciation?.all

            if (phonetic != null) {
                Log.d("WordRepository", "Fetched pronunciation for $word: $phonetic")
                return@withContext phonetic
            } else {
                Log.d("WordRepository", "No pronunciation found for $word")
                return@withContext null
            }

        } catch (e: Exception) {
            Log.e("WordRepository", "Error fetching pronunciation: ${e.message}", e)
            return@withContext null
        }
    }

    suspend fun getLearningWordIds(): List<String> = withContext(Dispatchers.IO) {
        firebaseAuth.currentUser?.let { user ->
            try {
                firestore.collection(COLLECTION_USER_WORDS)
                    .document(user.uid)
                    .collection("words")
                    .whereEqualTo("status", WordLearningStatus.LEARNING.name)
                    .get()
                    .await()
                    .documents
                    .mapNotNull { it.id }
            } catch (e: Exception) {
                Log.e("WordRepository", "Error fetching learning word IDs: ${e.message}")
                emptyList()
            }
        } ?: emptyList()
    }
}
