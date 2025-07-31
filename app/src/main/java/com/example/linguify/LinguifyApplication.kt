package com.example.linguify

import android.app.Application
import android.util.Log
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.example.linguify.data.repositories.PexelsRepository
import com.example.linguify.data.repositories.WordRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class LinguifyApplication : Application() {
    @Inject
    lateinit var firebaseAuth: FirebaseAuth

    @Inject
    lateinit var firebaseFirestore: FirebaseFirestore

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    @Inject
    lateinit var wordRepository: WordRepository

    override fun onCreate() {
        super.onCreate()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                wordRepository.checkAndReloadDatabase()

                preloadCommonData()
            } catch (e: Exception) {
                Log.e("LinguifyApplication", "Error initializing database: ${e.message}")
            }
        }
    }

    private suspend fun preloadCommonData() {
        try {
            val pexelsRepository = PexelsRepository()

            val categories = listOf("language", "learning", "education", "book", "study")
            categories.forEach { category ->
                try {
                    val photo = pexelsRepository.searchPhotoForWord(category)
                } catch (e: Exception) {
                    Log.e("LinguifyApplication", "Error preloading image for $category: ${e.message}")
                }
            }
        } catch (e: Exception) {
            Log.e("LinguifyApplication", "Error preloading common data: ${e.message}")
        }
    }
}
