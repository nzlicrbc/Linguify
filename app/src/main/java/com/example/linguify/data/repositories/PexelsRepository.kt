package com.example.linguify.data.repositories

import android.util.Log
import com.example.linguify.data.remote.PexelsApiClient
import com.example.linguify.data.remote.model.PexelsPhoto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PexelsRepository @Inject constructor() {

    companion object {
        private val imageCache = mutableMapOf<String, PexelsPhoto?>()
        private const val CACHE_SIZE_LIMIT = 100
    }

    private val apiService = PexelsApiClient.apiService
    private val apiKey = PexelsApiClient.apiKey

    suspend fun searchPhotoForWord(word: String): PexelsPhoto? {
        if (imageCache.containsKey(word)) {
            val cachedPhoto = imageCache[word]
            Log.d("PexelsRepository", "Using cached image for word: $word")
            return cachedPhoto
        }

        return try {
            val response = apiService.searchPhotos(
                apiKey = apiKey,
                query = word,
                perPage = 1
            )

            val photo = if (response.photos.isNotEmpty()) {
                response.photos[0]
            } else {
                null
            }

            if (imageCache.size >= CACHE_SIZE_LIMIT) {
                val firstKey = imageCache.keys.firstOrNull()
                firstKey?.let {
                    imageCache.remove(it)
                }
            }

            imageCache[word] = photo

            photo
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun getRandomImages(count: Int): List<PexelsPhoto> {
        val categories = listOf(
            "nature", "travel", "technology", "food", "business",
            "animals", "architecture", "arts", "education", "health"
        )

        val resultPhotos = mutableListOf<PexelsPhoto>()

        try {
            val cachedCategories = categories.filter { imageCache.containsKey(it) }
            val shuffledCategories = cachedCategories.shuffled()

            shuffledCategories.take(count).forEach { category ->
                imageCache[category]?.let {
                    resultPhotos.add(it)
                }
            }

            val remainingCount = count - resultPhotos.size
            if (remainingCount > 0) {
                val categoriesToFetch = categories.filterNot { cachedCategories.contains(it) }.shuffled()

                for (i in 0 until remainingCount) {
                    if (i < categoriesToFetch.size) {
                        val category = categoriesToFetch[i]
                        val response = apiService.searchPhotos(
                            apiKey = apiKey,
                            query = category,
                            perPage = 1,
                            page = (1..10).random()
                        )

                        if (response.photos.isNotEmpty()) {
                            val photo = response.photos[0]
                            resultPhotos.add(photo)

                            imageCache[category] = photo
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PexelsRepository", "Error loading random images: ${e.message}")
        }

        return resultPhotos
    }
}
