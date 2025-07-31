class HomeViewModel @Inject constructor(
    private val wordRepository: WordRepository,
    private val userPreferencesRepository: UserPreferencesRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {
    // Yeni state ekleyelim
    private val _randomWordImage = MutableStateFlow<String>("")
    val randomWordImage: StateFlow<String> = _randomWordImage

    // Rastgele kelime resmi getiren fonksiyon
    private fun loadRandomWordImage() {
        viewModelScope.launch {
            try {
                val randomWord = wordRepository.getRandomDiscoverableWord(userLevel)
                randomWord?.let { word ->
                    _randomWordImage.value = word.imageUrl ?: ""
                }
            } catch (e: Exception) {
                Log.e("HomeViewModel", "Error loading random word image: ${e.message}", e)
            }
        }
    }

    // init bloğuna veya loadWordCounts fonksiyonuna ekleyin
    fun loadWordCounts() {
        viewModelScope.launch {
            // ... existing code ...
            
            loadRandomWordImage() // Rastgele kelime resmini yükle
            
            // ... existing code ...
        }
    }
} 