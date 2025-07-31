package com.example.linguify.ui.worddetail

import android.app.Application
import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.linguify.data.repositories.PexelsRepository
import com.example.linguify.data.repositories.WordRepository
import com.example.linguify.model.Word
import com.example.linguify.model.WordLearningStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class WordDetailViewModel @Inject constructor(
    private val wordRepository: WordRepository,
    private val pexelsRepository: PexelsRepository,
    private val application: Application,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _wordState = MutableStateFlow<WordState>(WordState.Loading)
    val wordState: StateFlow<WordState> = _wordState

    private val _wordImageUrl = MutableStateFlow<String?>(null)
    val wordImageUrl: StateFlow<String?> = _wordImageUrl

    private val _pronunciationState = MutableStateFlow<PronunciationState>(PronunciationState.Idle)
    val pronunciationState: StateFlow<PronunciationState> = _pronunciationState

    private val _currentWord = MutableStateFlow<Word?>(null)
    val currentWord: StateFlow<Word?> = _currentWord

    private var mediaPlayer: MediaPlayer? = null
    private var wordId: String = savedStateHandle.get<String>("wordId") ?: ""

    private var textToSpeech: TextToSpeech? = null
    private var ttsInitialized = false

    init {
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
        }

        loadWord()

        initTextToSpeech(application)
    }

    private fun initTextToSpeech(context: Context) {
        textToSpeech = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                val result = textToSpeech?.setLanguage(Locale.US)
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("WordDetailViewModel", "Language not supported")
                } else {
                    ttsInitialized = true
                    Log.d("WordDetailViewModel", "TTS initialized successfully")
                }
            } else {
                Log.e("WordDetailViewModel", "TTS initialization failed")
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

    private fun loadWord() {
        viewModelScope.launch {
            _wordState.value = WordState.Loading
            try {
                val userId = wordId.split("_").firstOrNull()

                val savedWords = wordRepository.getSavedWordsFromFirebase()
                val savedWord = savedWords.find { it.id == wordId }

                if (savedWord != null) {
                    _wordState.value = WordState.Success(savedWord)
                    _currentWord.value = savedWord

                    if (savedWord.synonyms.isEmpty() || savedWord.definition.isNullOrEmpty()) {
                        fetchWordDetailsFromApi(savedWord.text)
                    }

                    if (savedWord.imageUrl == null) {
                        loadWordImageFromPexels(savedWord.text)
                    }
                } else {
                    val wordEntities = wordRepository.getWordsByIds(listOf(wordId))
                    if (wordEntities.isNotEmpty()) {
                        val words = wordRepository.fetchWordDetailsAndMapToWord(wordEntities)
                        if (words.isNotEmpty()) {
                            val word = words.first()
                            _wordState.value = WordState.Success(word)
                            _currentWord.value = word

                            if (word.synonyms.isEmpty() || word.definition.isNullOrEmpty()) {
                                fetchWordDetailsFromApi(word.text)
                            }

                            if (word.imageUrl == null) {
                                loadWordImageFromPexels(word.text)
                            }
                        } else {
                            _wordState.value = WordState.Error("Word details not found")
                        }
                    } else {
                        _wordState.value = WordState.Error("Word not found")
                    }
                }
            } catch (e: Exception) {
                Log.e("WordDetailViewModel", "Error loading word: ${e.message}", e)
                _wordState.value = WordState.Error(e.message ?: "An error occurred")
            }
        }
    }


    private fun fetchWordDetailsFromApi(wordText: String) {
        viewModelScope.launch {
            try {
                Log.d("WordDetailViewModel", "Fetching details from API for: $wordText")

                val wordDetail = wordRepository.fetchWordDetailsFromApi(wordText)

                if (wordDetail != null) {
                    (wordState.value as? WordState.Success)?.word?.let { currentWord ->
                        val updatedWord = currentWord.copy(
                            definition = wordDetail.definition ?: currentWord.definition,
                            example = wordDetail.examples.firstOrNull() ?: currentWord.example,
                            synonyms = if (wordDetail.synonyms.isNotEmpty()) wordDetail.synonyms else currentWord.synonyms,
                            antonyms = if (wordDetail.antonyms.isNotEmpty()) wordDetail.antonyms else currentWord.antonyms,
                            phoneticSpelling = wordDetail.phoneticSpelling ?: currentWord.phoneticSpelling,
                            wordType = wordDetail.wordType ?: currentWord.wordType
                        )

                        _wordState.value = WordState.Success(updatedWord)
                        _currentWord.value = updatedWord

                        wordRepository.saveWordToFirebase(updatedWord)

                        Log.d("WordDetailViewModel", "Word details updated from API: ${updatedWord.text}")
                    }
                }
            } catch (e: Exception) {
                Log.e("WordDetailViewModel", "Error fetching word details from API: ${e.message}", e)
            }
        }
    }

    fun loadWordImageFromPexels(wordText: String) {
        viewModelScope.launch {
            try {
                val photo = pexelsRepository.searchPhotoForWord(wordText)

                if (photo != null) {
                    val imageUrl = photo.src.medium
                    _wordImageUrl.value = imageUrl

                    (wordState.value as? WordState.Success)?.word?.let { word ->
                        val updatedWord = word.copy(imageUrl = imageUrl)
                        _currentWord.value = updatedWord

                        saveWordWithImage(updatedWord)
                    }
                } else {
                    val alternativeWords = getAlternativeSearchTerms(wordText)
                    var foundImage = false

                    for (altWord in alternativeWords) {
                        val altPhoto = pexelsRepository.searchPhotoForWord(altWord)
                        if (altPhoto != null) {
                            val imageUrl = altPhoto.src.medium
                            _wordImageUrl.value = imageUrl

                            (wordState.value as? WordState.Success)?.word?.let { word ->
                                val updatedWord = word.copy(imageUrl = imageUrl)
                                _currentWord.value = updatedWord

                                saveWordWithImage(updatedWord)
                            }

                            foundImage = true
                            break
                        }
                    }

                    if (!foundImage) {
                        Log.e("WordDetailViewModel", "No image found for word: $wordText")
                        _wordImageUrl.value = null
                    }
                }
            } catch (e: Exception) {
                Log.e("WordDetailViewModel", "Error loading image from Pexels: ${e.message}", e)
                _wordImageUrl.value = null
            }
        }
    }

    private fun saveWordWithImage(word: Word) {
        viewModelScope.launch {
            try {
                wordRepository.saveWordToFirebase(word)
                Log.d("WordDetailViewModel", "Word saved to Firebase with image URL: ${word.text}")

                _wordState.value = WordState.Success(word)
                _currentWord.value = word
            } catch (e: Exception) {
                Log.e("WordDetailViewModel", "Error saving word with image to Firebase: ${e.message}")
            }
        }
    }

    private fun getAlternativeSearchTerms(word: String): List<String> {
        val parts = word.split(Regex("(?<=[a-z])(?=[A-Z])")).map { it.toLowerCase() }

        val alternatives = mutableListOf<String>()
        alternatives.add(word)

        when (word.toLowerCase()) {
            "aim" -> alternatives.addAll(listOf("target", "goal", "archery"))
            "success" -> alternatives.addAll(listOf("achievement", "trophy", "win"))
            "knowledge" -> alternatives.addAll(listOf("books", "library", "education"))
            "love" -> alternatives.addAll(listOf("heart", "couple", "romance"))
            "beauty" -> alternatives.addAll(listOf("flower", "nature", "landscape"))
            else -> {
                if (word.contains(" ")) {
                    alternatives.addAll(word.split(" "))
                }

                if (parts.size > 1) {
                    alternatives.addAll(parts)
                }

                alternatives.add("$word concept")
                alternatives.add("$word illustration")
            }
        }

        return alternatives.distinct()
    }

    fun updateWordStatus(status: WordLearningStatus) {
        viewModelScope.launch {
            (wordState.value as? WordState.Success)?.word?.let { word ->
                try {
                    wordRepository.updateWordStatus(word.id, status)

                    val updatedWord = word.copy(
                        status = status,
                        imageUrl = word.imageUrl ?: _wordImageUrl.value
                    )
                    _wordState.value = WordState.Success(updatedWord)
                    _currentWord.value = updatedWord

                    wordRepository.saveWordToFirebase(updatedWord)
                } catch (e: Exception) {
                    Log.e("WordDetailViewModel", "Error updating word status: ${e.message}", e)
                }
            }
        }
    }

    fun playPronunciation() {
        (wordState.value as? WordState.Success)?.word?.let { word ->
            if (ttsInitialized) {
                _pronunciationState.value = PronunciationState.Playing
                textToSpeech?.speak(word.text, TextToSpeech.QUEUE_FLUSH, null, "wordPronunciation")
            } else {
                _pronunciationState.value = PronunciationState.Error
                Log.e("WordDetailViewModel", "TTS not initialized")
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

    sealed class WordState {
        object Loading : WordState()
        data class Success(val word: Word) : WordState()
        data class Error(val message: String) : WordState()
    }

    sealed class PronunciationState {
        object Idle : PronunciationState()
        object Playing : PronunciationState()
        object Error : PronunciationState()
    }
}
