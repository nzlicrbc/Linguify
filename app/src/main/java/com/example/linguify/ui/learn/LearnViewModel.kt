package com.example.linguify.ui.learn

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.linguify.data.repositories.UserPreferencesRepository
import com.example.linguify.data.repositories.PexelsRepository
import com.example.linguify.data.repositories.WordRepository
import com.example.linguify.model.Word
import com.example.linguify.model.WordDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class LearnViewModel @Inject constructor(
    private val wordRepository: WordRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val pexelsRepository: PexelsRepository,
    private val application: Application
) : ViewModel() {

    private val _loadingState = MutableStateFlow<LoadingState>(LoadingState.Loading)
    val loadingState: StateFlow<LoadingState> = _loadingState

    private val _currentWord = MutableStateFlow<Word?>(null)
    val currentWord: StateFlow<Word?> = _currentWord

    private val _nextWord = MutableStateFlow<Word?>(null)
    val nextWord: StateFlow<Word?> = _nextWord

    private val _wordPosition = MutableStateFlow(0)
    val wordPosition: StateFlow<Int> = _wordPosition

    private val _pronunciationState = MutableStateFlow<PronunciationState>(PronunciationState.Idle)
    val pronunciationState: StateFlow<PronunciationState> = _pronunciationState

    private val _wordImageUrl = MutableStateFlow<String?>(null)
    val wordImageUrl: StateFlow<String?> = _wordImageUrl

    private val _hasWords = MutableStateFlow(false)
    val hasWords: StateFlow<Boolean> = _hasWords

    private val _wordsToLearn = mutableListOf<Word>()
    private var currentIndex = 0

    private var mediaPlayer: MediaPlayer? = null

    private var textToSpeech: TextToSpeech? = null
    private var ttsInitialized = false

    private val imageCache = mutableMapOf<String, String?>()
    private val wordDetailCache = mutableMapOf<String, WordDetail?>()

    private val _nextWordImageUrl = MutableStateFlow<String?>(null)
    val nextWordImageUrl: StateFlow<String?> = _nextWordImageUrl

    init {
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
        }

        viewModelScope.launch(Dispatchers.IO) {
            preloadWordsToLearn()
        }

        loadWordsToLearn()

        viewModelScope.launch {
            if (_nextWord.value != null && _nextWordImageUrl.value == null) {
                loadImageForNextWord()
            }
        }

        initTextToSpeech(application)
    }

    private suspend fun preloadWordsToLearn() {
        try {
            val userLevel = userPreferencesRepository.getUserLevel()
            val toLearnWords = wordRepository.getToLearnWords(userLevel.code)

            if (toLearnWords.isNotEmpty()) {
                toLearnWords.take(5).forEach { word ->
                    wordRepository.fetchWordDetailsFromApi(word.text)?.let { detail ->
                        wordDetailCache[word.text] = detail
                    }

                    if (word.imageUrl.isNullOrEmpty()) {
                        val photo = pexelsRepository.searchPhotoForWord(word.text)
                        photo?.let {
                            imageCache[word.text] = it.src.medium
                        }
                    } else {
                        imageCache[word.text] = word.imageUrl
                    }
                }

                Log.d("LearnViewModel", "Preloaded ${toLearnWords.take(5).size} words to learn")
            }
        } catch (e: Exception) {
            Log.e("LearnViewModel", "Error preloading words to learn: ${e.message}")
        }
    }

    private fun initTextToSpeech(context: Context) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("LearnViewModel", "Language not supported")
                } else {
                    ttsInitialized = true
                    Log.d("LearnViewModel", "TTS initialized successfully")
                }
            } else {
                Log.e("LearnViewModel", "TTS initialization failed")
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

    fun loadWordsToLearn() {
        _loadingState.value = LoadingState.Loading
        viewModelScope.launch {
            try {
                val userLevel = userPreferencesRepository.getUserLevel()
                val toLearnWords = wordRepository.getToLearnWords(userLevel.code)

                _hasWords.value = toLearnWords.isNotEmpty()

                if (_hasWords.value) {
                    _wordsToLearn.clear()
                    _wordsToLearn.addAll(toLearnWords)
                    currentIndex = 0
                    updateCurrentAndNextWord()
                    _loadingState.value = LoadingState.Success
                } else {
                    _currentWord.value = null
                    _nextWord.value = null
                    _wordPosition.value = 0
                    _loadingState.value = LoadingState.Empty
                }

                saveNextWordToPrefs()
            } catch (e: Exception) {
                Log.e("LearnViewModel", "Error loading words to learn: ${e.message}")
                _hasWords.value = false
                _loadingState.value = LoadingState.Error(e.message ?: "An error occurred")
            }
        }
    }

    private fun updateCurrentAndNextWord() {
        if (_wordsToLearn.isEmpty()) {
            _currentWord.value = null
            _nextWord.value = null
            _wordPosition.value = 0
            return
        }

        _currentWord.value = _wordsToLearn[currentIndex]
        _wordPosition.value = currentIndex + 1

        loadImageForCurrentWord()
        fetchPhoneticFromApi(_currentWord.value?.text)

        _nextWord.value = _wordsToLearn[0]

        saveNextWordToPrefs()

        loadImageForNextWord()
    }

    private fun loadImageForNextWord() {
        viewModelScope.launch {
            _nextWord.value?.let { nextWord ->
                try {
                    val photo = pexelsRepository.searchPhotoForWord(nextWord.text)
                    _nextWordImageUrl.value = photo?.src?.medium
                } catch (e: Exception) {
                    Log.e("LearnViewModel", "Error loading next word image: ${e.message}")
                }
            }
        }
    }

    private fun loadImageForCurrentWord() {
        viewModelScope.launch {
            _currentWord.value?.let { word ->
                try {
                    if (imageCache.containsKey(word.text)) {
                        _wordImageUrl.value = imageCache[word.text]
                        Log.d("LearnViewModel", "Loaded image from cache for word: ${word.text}")
                        return@launch
                    }

                    if (!word.imageUrl.isNullOrEmpty()) {
                        _wordImageUrl.value = word.imageUrl
                        imageCache[word.text] = word.imageUrl
                        Log.d("LearnViewModel", "Using existing image URL for word: ${word.text}")
                        return@launch
                    }

                    Log.d("LearnViewModel", "Attempting to load image for word: ${word.text}")
                    val photo = pexelsRepository.searchPhotoForWord(word.text)
                    Log.d("LearnViewModel", "Photo found: ${photo?.src?.medium}")

                    val imageUrl = photo?.src?.medium
                    _wordImageUrl.value = imageUrl

                    imageCache[word.text] = imageUrl
                } catch (e: Exception) {
                    Log.e("LearnViewModel", "Image loading error: ${e.message}")
                    _wordImageUrl.value = null
                }
            }
        }
    }

    fun moveToNextWord() {
        if (_wordsToLearn.isEmpty()) {
            _hasWords.value = false
            return
        }

        if (currentIndex < _wordsToLearn.size - 1) {
            currentIndex++
        } else {
            currentIndex = 0
        }

        updateCurrentAndNextWord()
    }

    private fun saveNextWordToPrefs() {
        viewModelScope.launch {
            _nextWord.value?.let { word ->
                userPreferencesRepository.saveNextWordToLearn(word.text)
            } ?: run {
                userPreferencesRepository.saveNextWordToLearn("")
            }
        }
    }

    fun playPronunciation() {
        _currentWord.value?.let { word ->
            if (ttsInitialized) {
                _pronunciationState.value = PronunciationState.Playing

                val bundle = Bundle()
                bundle.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "wordPronunciation")

                textToSpeech?.speak(word.text, TextToSpeech.QUEUE_FLUSH, bundle, "wordPronunciation")
            } else {
                _pronunciationState.value = PronunciationState.Error
                Log.e("LearnViewModel", "TTS not initialized")
            }
        }
    }

    private fun fetchPhoneticFromApi(wordText: String?) {
        if (wordText == null) return

        viewModelScope.launch {
            try {
                Log.d("LearnViewModel", "Fetching phonetic for: $wordText")

                val savedWords = wordRepository.getSavedWordsFromFirebase()
                val savedWord = savedWords.find { it.text.equals(wordText, ignoreCase = true) }

                if (savedWord != null && !savedWord.phoneticSpelling.isNullOrEmpty()) {
                    Log.d("LearnViewModel", "Found phonetic in saved words: ${savedWord.phoneticSpelling}")

                    _currentWord.value?.let { currentWord ->
                        val updatedWord = currentWord.copy(phoneticSpelling = savedWord.phoneticSpelling)
                        _currentWord.value = updatedWord
                    }
                    return@launch
                }

                val phonetic = wordRepository.fetchWordPronunciation(wordText)

                if (phonetic != null) {
                    _currentWord.value?.let { currentWord ->
                        val updatedWord = currentWord.copy(phoneticSpelling = phonetic)
                        _currentWord.value = updatedWord

                        wordRepository.saveWordToFirebase(updatedWord)

                        Log.d("LearnViewModel", "Updated phonetic from API: $phonetic")
                    }
                }
            } catch (e: Exception) {
                Log.e("LearnViewModel", "Error fetching phonetic: ${e.message}", e)
            }
        }
    }

    override fun onCleared() {
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        mediaPlayer?.release()
        mediaPlayer = null
        super.onCleared()
    }

    sealed class LoadingState {
        object Loading : LoadingState()
        object Success : LoadingState()
        object Empty : LoadingState()
        data class Error(val message: String) : LoadingState()
    }

    sealed class PronunciationState {
        object Idle : PronunciationState()
        object Playing : PronunciationState()
        object Error : PronunciationState()
    }
}
