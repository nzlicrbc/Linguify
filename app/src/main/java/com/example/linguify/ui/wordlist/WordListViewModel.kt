package com.example.linguify.ui.wordlist

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.linguify.data.repositories.PexelsRepository
import com.example.linguify.data.repositories.UserPreferencesRepository
import com.example.linguify.data.repositories.WordRepository
import com.example.linguify.model.Word
import com.example.linguify.model.WordLearningStatus
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class WordListViewModel @Inject constructor(
    private val wordRepository: WordRepository,
    private val pexelsRepository: PexelsRepository,
    private val userPreferencesRepository: UserPreferencesRepository
) : ViewModel() {

    private val _wordListState = MutableStateFlow<WordListState>(WordListState.Loading)
    val wordListState: StateFlow<WordListState> = _wordListState

    private val _navigateToWordDetail = MutableStateFlow<String?>(null)
    val navigateToWordDetail: StateFlow<String?> = _navigateToWordDetail

    private val imageCache = mutableMapOf<String, String?>()

    private var allWords: List<Word> = emptyList()
    private var currentSearchQuery: String = ""
    private var currentListType: String = ""

    private var cachedLearningWords: List<Word> = emptyList()
    private var cachedUserLevelCode: String? = null

    private val pageSize = 15
    private var isLoading = false
    private var hasMoreData = true

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore


    fun onWordClicked(word: Word) {
        _navigateToWordDetail.value = word.id
        Log.d("WordListViewModel", "Word clicked: ${word.text}, ID: ${word.id}")
    }

    fun onWordDetailNavigated() {
        _navigateToWordDetail.value = null
    }

    fun loadWords(listType: String, reset: Boolean = true) {
        if (isLoading) return

        if (reset) {
            allWords = emptyList()
            _wordListState.value = WordListState.Loading
            currentListType = listType
        } else {
            if (!hasMoreData) return
        }

        isLoading = true

        viewModelScope.launch {
            try {
                val userLevelCode = cachedUserLevelCode
                    ?: userPreferencesRepository.getUserLevel().code.also { cachedUserLevelCode = it }

                val offset = if (reset) 0 else allWords.size

                val newWords = when (listType) {
                    "known" -> wordRepository.getKnownWordsPaged(userLevelCode, pageSize, offset)
                    "to_learn" -> wordRepository.getToLearnWordsPaged(
                        userLevelCode,
                        pageSize,
                        offset
                    )

                    "learning" -> {
                        if (reset) {
                            val allSavedWords = wordRepository.getSavedWordsFromFirebase()
                            val learningWords = allSavedWords.filter {
                                it.status == WordLearningStatus.LEARNING
                            }

                            cachedLearningWords = learningWords

                            learningWords.take(pageSize)
                        } else {
                            cachedLearningWords
                                .drop(offset)
                                .take(pageSize)
                        }
                    }

                    else -> emptyList()
                }

                hasMoreData = newWords.size == pageSize

                val updatedWords = if (reset) newWords else allWords + newWords
                allWords = updatedWords

                if (currentSearchQuery.isNotEmpty()) {
                    searchWords(currentSearchQuery)
                } else {
                    _wordListState.value = WordListState.Success(updatedWords)
                }

                Log.d(
                    "WordListViewModel",
                    "Loaded ${newWords.size} words for $listType, total: ${updatedWords.size}"
                )

            } catch (e: Exception) {
                Log.e("WordListViewModel", "Error loading words: ${e.message}", e)
                _wordListState.value = WordListState.Error("Failed to load words: ${e.message}")
            } finally {
                isLoading = false
                _isLoadingMore.value = false
            }
        }
    }

    fun loadMoreWords() {
        if (isLoading || !hasMoreData) return

        _isLoadingMore.value = true

        loadWords(currentListType, false)

    }

    fun loadImageForWord(word: String, callback: (String?) -> Unit) {
        if (imageCache.containsKey(word)) {
            callback(imageCache[word])
            return
        }

        viewModelScope.launch {
            try {
                val photo = pexelsRepository.searchPhotoForWord(word)
                val imageUrl = photo?.src?.medium

                imageCache[word] = imageUrl

                callback(imageUrl)
            } catch (e: Exception) {
                Log.e("WordListViewModel", "Error loading image for $word: ${e.message}")
                callback(null)
            }
        }
    }

    fun searchWords(query: String) {
        viewModelScope.launch {
            currentSearchQuery = query

            if (query.isEmpty()) {
                _wordListState.value = WordListState.Success(allWords)
                return@launch
            }

            val lowerCaseQuery = query.lowercase()
            val filteredWords = allWords.filter { word ->
                word.text.lowercase().contains(lowerCaseQuery) ||
                        (word.translation?.lowercase()?.contains(lowerCaseQuery) ?: false)
            }

            _wordListState.value = WordListState.Success(filteredWords)
        }
    }

    sealed class WordListState {
        object Loading : WordListState()
        data class Success(val words: List<Word>) : WordListState()
        data class Error(val message: String) : WordListState()
    }
}
