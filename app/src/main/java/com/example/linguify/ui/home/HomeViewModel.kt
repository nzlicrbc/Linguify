package com.example.linguify.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.linguify.data.manager.StreakManager
import com.example.linguify.data.repositories.PexelsRepository
import com.example.linguify.data.repositories.UserPreferencesRepository
import com.example.linguify.data.repositories.WordRepository
import com.example.linguify.model.StreakData
import com.example.linguify.model.Word
import com.example.linguify.model.WordLearningStatus
import com.example.linguify.utils.UserLevel
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val wordRepository: WordRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val firebaseAuth: FirebaseAuth,
    private val pexelsRepository: PexelsRepository,
    private val streakManager: StreakManager
) : ViewModel() {

    private val _wordCountsState = MutableStateFlow<WordCountsState>(WordCountsState.Loading)
    val wordCountsState: StateFlow<WordCountsState> = _wordCountsState

    private var userLevel: UserLevel = UserLevel.BEGINNER

    private var wordCountsListener: Any? = null

    private val _nextWordToLearn = MutableStateFlow("")
    val nextWordToLearn: StateFlow<String> = _nextWordToLearn

    private val _learningWordsCount = MutableStateFlow(0)
    val learningWordsCount: StateFlow<Int> = _learningWordsCount

    private val _discoverableWordCount = MutableStateFlow(0)
    val discoverableWordCount: StateFlow<Int> = _discoverableWordCount

    private val _discoverImages = MutableStateFlow<List<String>>(emptyList())
    val discoverImages: StateFlow<List<String>> = _discoverImages

    private val _username = MutableStateFlow("")
    val username: StateFlow<String> = _username

    private val _nextWordImageUrl = MutableStateFlow<String?>(null)
    val nextWordImageUrl: StateFlow<String?> = _nextWordImageUrl

    private val _learningWordsForReview = MutableStateFlow<List<Word>>(emptyList())
    val learningWordsForReview: StateFlow<List<Word>> = _learningWordsForReview

    private val imageCache = mutableMapOf<String, String>()

    private val _streakData = MutableStateFlow(StreakData())
    val streakData: StateFlow<StreakData> = _streakData

    init {
        viewModelScope.launch(Dispatchers.IO) {
            preloadCommonImages()
        }

        viewModelScope.launch {
            try {
                val dbStatus = wordRepository.checkAndReloadDatabase()
                Log.d("HomeViewModel", "Database status check completed: $dbStatus")

                if (!dbStatus) {
                    _wordCountsState.value = WordCountsState.Error("Database could not be prepared.")
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error checking database: ${e.message}", e)
            }
        }

        viewModelScope.launch {
            loadStreakData()
        }
    }

    private suspend fun loadStreakData() {
        try {
            streakManager.recordDailyActivity()

            val weeklyStreak = streakManager.getWeeklyStreak()
            val currentStreak = streakManager.getCurrentStreak()
            val lastActivityDate = streakManager.getLastActivityDate()

            _streakData.value = StreakData(
                streakDays = weeklyStreak,
                currentStreakCount = currentStreak,
                lastActivityDate = lastActivityDate
            )

            Log.d("HomeViewModel", "Streak data loaded - Current: $currentStreak")

        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error loading streak data: ${e.message}")
        }
    }

    fun refreshStreakData() {
        viewModelScope.launch {
            loadStreakData()
        }
    }

    private suspend fun preloadCommonImages() {
        try {
            val photos = pexelsRepository.getRandomImages(4)
            photos.forEach { photo ->
                imageCache[photo.id.toString()] = photo.src.medium
            }
            _discoverImages.value = photos.map { it.src.medium }
        } catch (e: Exception) {
            Log.e("HomeViewModel", "Error preloading images: ${e.message}")
        }
    }

    private fun loadUsername() {
        viewModelScope.launch {
            val currentUser = firebaseAuth.currentUser
            currentUser?.email?.let { email ->
                val username = email.split("@").firstOrNull() ?: ""
                _username.value = username
            }
        }
    }

    fun loadNextWordImage(wordText: String) {
        viewModelScope.launch {
            try {
                val photo = pexelsRepository.searchPhotoForWord(wordText)
                _nextWordImageUrl.value = photo?.src?.medium
                Log.d("HomeViewModel", "Loaded next word image for: $wordText")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading next word image: ${e.message}")
                _nextWordImageUrl.value = null
            }
        }
    }

    fun loadLearningWordsForReview() {
        viewModelScope.launch {
            try {
                val savedWordsCache = viewModelScope.async {
                    wordRepository.getSavedWordsFromFirebase()
                }

                val savedWords = savedWordsCache.await()
                val learningWords = savedWords.filter { it.status == WordLearningStatus.LEARNING }

                Log.d("HomeViewModel", "Filtered ${learningWords.size} learning words")

                _learningWordsCount.value = learningWords.size

                val selectedWords = if (learningWords.size > 3) {
                    learningWords.shuffled().take(3)
                } else {
                    learningWords.shuffled()
                }

                _learningWordsForReview.value = selectedWords

                val wordsWithImages = selectedWords.map { word ->
                    viewModelScope.async {
                        if (word.imageUrl.isNullOrEmpty()) {
                            try {
                                if (imageCache.containsKey(word.text)) {
                                    val updatedWord = word.copy(imageUrl = imageCache[word.text])
                                    wordRepository.saveWordToFirebase(updatedWord)
                                    updatedWord
                                } else {
                                    val photo = pexelsRepository.searchPhotoForWord(word.text)
                                    if (photo != null) {
                                        val updatedWord = word.copy(imageUrl = photo.src.medium)
                                        imageCache[word.text] = photo.src.medium
                                        wordRepository.saveWordToFirebase(updatedWord)
                                        updatedWord
                                    } else {
                                        word
                                    }
                                }
                            } catch (e: Exception) {
                                word
                            }
                        } else {
                            word
                        }
                    }
                }.awaitAll()

                _learningWordsForReview.value = wordsWithImages
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading learning words: ${e.message}")
                _learningWordsForReview.value = emptyList()
                _learningWordsCount.value = 0
            }
        }
    }

    fun loadWordCounts() {
        viewModelScope.launch {
            _wordCountsState.value = WordCountsState.Loading
            loadUsername()
            try {
                userLevel = userPreferencesRepository.getUserLevel()
                Log.d("HomeViewModel", "Setting up real-time word counts for level: ${userLevel.code}")

                removeWordCountsListener()

                wordCountsListener = wordRepository.listenToWordCounts(
                    userLevel = userLevel,
                    onUpdate = { counts ->
                        _wordCountsState.value = WordCountsState.Success(
                            totalWords = counts.totalWords,
                            knownWords = counts.knownWords,
                            toLearnWords = counts.toLearnWords,
                            learningWords = counts.learningWords,
                            reviewWords = counts.reviewWords
                        )

                        viewModelScope.launch {
                            _discoverableWordCount.value = wordRepository.getDiscoverableWordCount(userLevel)
                        }
                    },
                    onError = { error ->
                        Log.e("HomeViewModel", "Error in word counts listener: $error")
                        _wordCountsState.value = WordCountsState.Error("Error retrieving word counts: $error")
                    }
                )

                val toLearnWords = wordRepository.getToLearnWords(userLevel.code)
                if (toLearnWords.isNotEmpty()) {
                    val firstWord = toLearnWords[0].text
                    _nextWordToLearn.value = firstWord
                    userPreferencesRepository.saveNextWordToLearn(firstWord)
                    loadNextWordImage(firstWord)
                } else {
                    _nextWordToLearn.value = ""
                }

                val nextWord = userPreferencesRepository.getNextWordToLearn()

                if (nextWord.isEmpty()) {
                    val toLearnWords = wordRepository.getToLearnWords(userLevel.code)
                    if (toLearnWords.isNotEmpty()) {
                        val firstWord = toLearnWords[0].text
                        _nextWordToLearn.value = firstWord
                        userPreferencesRepository.saveNextWordToLearn(firstWord)
                        loadNextWordImage(firstWord)
                    } else {
                        _nextWordToLearn.value = ""
                        _nextWordImageUrl.value = null
                    }
                } else {
                    _nextWordToLearn.value = nextWord
                    loadNextWordImage(nextWord)
                }

                _discoverableWordCount.value = wordRepository.getDiscoverableWordCount(userLevel)
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error setting up word counts listener: ${e.message}", e)
                _wordCountsState.value = WordCountsState.Error("Error loading word counts: ${e.message}")
            }
        }
    }

    fun loadRandomDiscoverImages() {
        if (imageCache.isNotEmpty()) {
            _discoverImages.value = imageCache.values.take(4).toList()
            return
        }

        viewModelScope.launch {
            try {
                val photos = pexelsRepository.getRandomImages(4)
                val imageUrls = photos.map { it.src.medium }

                photos.forEach { photo ->
                    imageCache[photo.id.toString()] = photo.src.medium
                }

                _discoverImages.value = imageUrls
                Log.d("HomeViewModel", "Loaded ${photos.size} random discover images")
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading discover images: ${e.message}")
            }
        }
    }

    private fun removeWordCountsListener() {
        wordCountsListener?.let {
            wordRepository.removeWordCountsListener(it)
            wordCountsListener = null
        }
    }

    override fun onCleared() {
        super.onCleared()
        removeWordCountsListener()
    }

    sealed class WordCountsState {
        object Loading : WordCountsState()
        data class Success(
            val totalWords: Int,
            val knownWords: Int,
            val toLearnWords: Int,
            val learningWords: Int,
            val reviewWords: Int
        ) : WordCountsState()
        data class Error(val message: String) : WordCountsState()
    }
}
