package com.example.linguify.ui.flashcard

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.linguify.data.repositories.UserPreferencesRepository
import com.example.linguify.data.repositories.WordRepository
import com.example.linguify.model.Word
import com.example.linguify.model.WordLearningStatus
import com.example.linguify.utils.UserLevel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.internal.Contexts.getApplication
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class FlashcardViewModel @Inject constructor(
    private val wordRepository: WordRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val firebaseAuth: FirebaseAuth,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _wordListState = MutableStateFlow<WordListState>(WordListState.Loading)
    val wordListState: StateFlow<WordListState> = _wordListState

    private val _currentWord = MutableStateFlow<Word?>(null)
    val currentWord: StateFlow<Word?> = _currentWord

    private val _pronunciationState = MutableStateFlow<PronunciationState>(PronunciationState.Idle)
    val pronunciationState: StateFlow<PronunciationState> = _pronunciationState

    private val _currentWordCount = MutableStateFlow(0)
    val currentWordCount: StateFlow<Int> = _currentWordCount

    private val _currentSetIndex = MutableStateFlow(1)
    private val _totalSets = MutableStateFlow(1)

    private val _wordsInCurrentSet = MutableStateFlow(0)
    val wordsInCurrentSet: StateFlow<Int> = _wordsInCurrentSet

    private var textToSpeech: TextToSpeech? = null
    private var ttsInitialized = false

    private val wordSetsCache = mutableMapOf<String, List<Word>>()

    private var words: List<Word> = emptyList()
    private var currentIndex = 0
    private val wordsPerSet = 20
    private var totalWordCount = 0

    private var mediaPlayer: MediaPlayer? = null

    private var userLevel: UserLevel = UserLevel.BEGINNER

    private val seenWordIds = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            val currentUserId = getCurrentUserId()
            val savedUserId = getSavedUserId()

            if (currentUserId != savedUserId) {
                clearCacheForCurrentUser()
                saveUserId(currentUserId)
            }
        }

        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setFlags(AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                    .build()
            )
        }

        viewModelScope.launch {
            try {
                val dbStatus = wordRepository.checkAndReloadDatabase()
                Log.d("FlashcardViewModel", "Database status check completed: $dbStatus")

                if (!dbStatus) {
                    _wordListState.value =
                        WordListState.Error("Database could not be prepared. Please restart the application.")
                }
            } catch (e: Exception) {
                Log.e("FlashcardViewModel", "Error checking database: ${e.message}", e)
            }
        }

        initTextToSpeech(context)
    }

    private fun getSavedUserId(): String? {
        return context.getSharedPreferences("flashcard_prefs", Context.MODE_PRIVATE)
            .getString("last_user_id", null)
    }

    private fun saveUserId(userId: String) {
        context.getSharedPreferences("flashcard_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("last_user_id", userId)
            .apply()
    }

    private fun getUserSpecificCacheKey(userLevel: UserLevel, setIndex: Int): String {
        val userId = getCurrentUserId()
        return "user_${userId}_level_${userLevel.code}_set_${setIndex}"
    }

    private fun getCurrentUserId(): String {
        return firebaseAuth.currentUser?.uid ?: "default_user"
    }

    private fun initTextToSpeech(context: Context) {
        if (ttsInitialized) return

        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("FlashcardViewModel", "Language not supported")
                } else {
                    ttsInitialized = true
                    Log.d("FlashcardViewModel", "TTS initialized successfully")
                }
            } else {
                Log.e("FlashcardViewModel", "TTS initialization failed")
            }
        }

        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _pronunciationState.value = PronunciationState.Playing
            }

            override fun onDone(utteranceId: String?) {
                _pronunciationState.value = PronunciationState.Idle
            }

            override fun onError(utteranceId: String?) {
                _pronunciationState.value = PronunciationState.Error
            }
        })
    }

    fun loadWordsForCurrentLevel() {
        viewModelScope.launch {
            _wordListState.value = WordListState.Loading

            try {
                userLevel = userPreferencesRepository.getUserLevel()
                val userId = getCurrentUserId()

                val cacheKey = getUserSpecificCacheKey(userLevel, _currentSetIndex.value)

                wordSetsCache[cacheKey]?.let { cachedWords ->
                    words = cachedWords
                    currentIndex = (wordRepository.getLastWordIndex(userLevel.code) % wordsPerSet).coerceIn(0, cachedWords.size - 1)
                    _wordsInCurrentSet.value = words.size.coerceAtMost(wordsPerSet)

                    if (words.isNotEmpty() && currentIndex < words.size) {
                        _currentWord.value = words[currentIndex]
                    }

                    _wordListState.value = WordListState.Success(
                        currentIndex = currentIndex,
                        totalWords = _wordsInCurrentSet.value,
                        setIndex = _currentSetIndex.value,
                        totalSets = _totalSets.value
                    )

                    Log.d("FlashcardViewModel", "Loaded ${cachedWords.size} words from cache for user: $userId, level: ${userLevel.code}")
                    return@launch
                }

                val dbReady = wordRepository.checkAndReloadDatabase()
                if (!dbReady) {
                    _wordListState.value = WordListState.Error("Veritabanı hazırlanamadı. Lütfen uygulamayı yeniden başlatın.")
                    return@launch
                }

                val lastWordIndex = wordRepository.getLastWordIndex(userLevel.code)
                _currentSetIndex.value = (lastWordIndex / wordsPerSet) + 1

                words = wordRepository.getWordsForLevel(userLevel)

                wordSetsCache[cacheKey] = words

                if (words.isEmpty()) {
                    _wordListState.value = WordListState.Error("No words found for this level.")
                    return@launch
                }

                val knownAndToLearnIds = wordRepository.getKnownWordIds() +
                        wordRepository.getToLearnWordIds() +
                        wordRepository.getLearningWordIds()

                val totalLevelWords = wordRepository.getWordCountByCefrLevels(userLevel.cefrLevels)

                val discoverableCount = totalLevelWords - knownAndToLearnIds.size

                Log.d("FlashcardViewModel", "Total level words: $totalLevelWords")
                Log.d("FlashcardViewModel", "Known & To Learn IDs count: ${knownAndToLearnIds.size}")
                Log.d("FlashcardViewModel", "Discoverable count: $discoverableCount")

                currentIndex = lastWordIndex % wordsPerSet
                if (currentIndex >= words.size) currentIndex = 0

                _wordsInCurrentSet.value = words.size.coerceAtMost(wordsPerSet)

                _totalSets.value = ((totalLevelWords + wordsPerSet - 1) / wordsPerSet).coerceAtLeast(1)

                if (words.isNotEmpty() && currentIndex < words.size) {
                    _currentWord.value = words[currentIndex]
                }

                _wordListState.value = WordListState.Success(
                    currentIndex = currentIndex,
                    totalWords = _wordsInCurrentSet.value,
                    setIndex = _currentSetIndex.value,
                    totalSets = _totalSets.value,
                    totalWordCount = totalLevelWords
                )
            } catch (e: Exception) {
                Log.e("FlashcardViewModel", "Error loading words: ${e.message}")
                _wordListState.value = WordListState.Error("An error occurred while loading words")
            }
        }
    }

    fun clearCacheForCurrentUser() {
        val userId = getCurrentUserId()
        val keysToRemove = wordSetsCache.keys.filter { it.startsWith("user_${userId}_") }
        keysToRemove.forEach { wordSetsCache.remove(it) }

        seenWordIds.clear()

        Log.d("FlashcardViewModel", "Cleared cache for user: $userId")
    }

    fun moveToNextWord() {
        if (currentIndex + 1 < words.size) {
            _currentWord.value?.let {
                seenWordIds.add(it.id)
            }

            currentIndex++
            _currentWord.value = words[currentIndex]

            saveLastWordIndex()

            _wordListState.value = WordListState.Success(
                currentIndex = currentIndex,
                totalWords = words.size.coerceAtMost(wordsPerSet),
                setIndex = _currentSetIndex.value,
                totalSets = _totalSets.value
            )

            Log.d(
                "FlashcardViewModel",
                "moveToNextWord - currentIndex: $currentIndex, totalWords: ${
                    words.size.coerceAtMost(wordsPerSet)
                }, setIndex: ${_currentSetIndex.value}"
            )

        } else {
            viewModelScope.launch {
                saveLastWordIndex()
                loadNextWordSet()
            }
        }
    }

    fun loadNextWordSet() {
        viewModelScope.launch {
            _wordListState.value = WordListState.Loading

            try {
                val lastIndex = wordRepository.getLastWordIndex(userLevel.code)
                val nextSetIndex = (lastIndex / wordsPerSet) + 1

                wordRepository.saveLastWordIndex(userLevel.code, nextSetIndex * wordsPerSet)
                _currentSetIndex.value = nextSetIndex + 1

                wordRepository.clearUserCache()

                currentIndex = 0
                loadWordsForCurrentLevel()

            } catch (e: Exception) {
                Log.e("FlashcardViewModel", "Error loading next set: ${e.message}")
                _wordListState.value = WordListState.Error("Sonraki set yüklenirken hata oluştu")
            }
        }
    }

    fun markWordStatus(word: Word, status: WordLearningStatus) {
        viewModelScope.launch {
            try {
                val updatedWord = word.copy(status = status)

                wordRepository.updateWordStatus(word.id, status)
                wordRepository.saveWordToFirebase(updatedWord)

                Log.d("FlashcardViewModel", "Marked word ${word.text} as ${status.name}")
            } catch (e: Exception) {
                Log.e("FlashcardViewModel", "Error marking word: ${e.message}")
            }
        }
    }

    fun markWordAsKnown(word: Word) {
        markWordStatus(word, WordLearningStatus.KNOWN)
    }

    fun markWordToLearn(word: Word) {
        markWordStatus(word, WordLearningStatus.TO_LEARN)
    }

    fun playPronunciation(word: Word) {
        if (ttsInitialized) {
            _pronunciationState.value = PronunciationState.Playing

            val bundle = Bundle()
            bundle.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "wordPronunciation")

            textToSpeech?.speak(word.text, TextToSpeech.QUEUE_FLUSH, bundle, "wordPronunciation")
        } else {
            word.pronunciationUrl?.let { url ->
                _pronunciationState.value = PronunciationState.Playing

                try {
                    mediaPlayer?.reset()
                    mediaPlayer?.setDataSource(url)
                    mediaPlayer?.prepareAsync()

                    mediaPlayer?.setOnPreparedListener {
                        it.start()
                    }

                    mediaPlayer?.setOnCompletionListener {
                        _pronunciationState.value = PronunciationState.Idle
                    }

                    mediaPlayer?.setOnErrorListener { _, _, _ ->
                        _pronunciationState.value = PronunciationState.Error
                        true
                    }
                } catch (e: Exception) {
                    _pronunciationState.value = PronunciationState.Error
                    Log.e("FlashcardViewModel", "Error playing pronunciation: ${e.message}")
                }
            } ?: run {
                _pronunciationState.value = PronunciationState.Error
            }
        }
    }

    fun saveLastWordIndex() {
        viewModelScope.launch {
            try {
                val currentSetStartIndex = (_currentSetIndex.value - 1) * wordsPerSet
                val globalIndex = currentSetStartIndex + currentIndex

                wordRepository.saveLastWordIndex(userLevel.code, globalIndex)
                Log.d("FlashcardViewModel", "Saved last word index: $globalIndex")
            } catch (e: Exception) {
                Log.e("FlashcardViewModel", "Error saving last word index: ${e.message}")
            }
        }
    }

    fun loadPreviousSet() {
        viewModelScope.launch {
            _wordListState.value = WordListState.Loading

            try {
                Log.d("FlashcardViewModel", "loadPreviousSet - Current set index: ${_currentSetIndex.value}")

                val currentSetIndex = _currentSetIndex.value

                if (currentSetIndex <= 1) {
                    _wordListState.value = WordListState.Error("Zaten ilk settesiniz")
                    return@launch
                }

                val previousSetIndex = currentSetIndex - 1
                _currentSetIndex.value = previousSetIndex

                Log.d("FlashcardViewModel", "loadPreviousSet - New set index: ${_currentSetIndex.value}")

                val previousSetStartIndex = (previousSetIndex - 1) * wordsPerSet

                wordRepository.saveLastWordIndex(userLevel.code, previousSetStartIndex)

                val previousCacheKey = getUserSpecificCacheKey(userLevel, _currentSetIndex.value)
                wordSetsCache.remove(previousCacheKey)

                currentIndex = 0
                loadWordsForCurrentLevel()

                _wordListState.value = WordListState.Success(
                    currentIndex = 0,
                    totalWords = _wordsInCurrentSet.value,
                    setIndex = _currentSetIndex.value,
                    totalSets = _totalSets.value,
                    totalWordCount = totalWordCount
                )

            } catch (e: Exception) {
                Log.e("FlashcardViewModel", "Error loading previous set: ${e.message}")
                _wordListState.value = WordListState.Error("Önceki set yüklenirken hata oluştu")
            }
        }
    }

    override fun onCleared() {
        mediaPlayer?.release()
        mediaPlayer = null
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        super.onCleared()
    }

    sealed class WordListState {
        object Loading : WordListState()
        data class Success(
            val currentIndex: Int,
            val totalWords: Int,
            val setIndex: Int,
            val totalSets: Int,
            val totalWordCount: Int = 0
        ) : WordListState()

        data class Error(val message: String) : WordListState()
        object Completed : WordListState()
    }

    sealed class PronunciationState {
        object Idle : PronunciationState()
        object Playing : PronunciationState()
        object Error : PronunciationState()
    }
}
