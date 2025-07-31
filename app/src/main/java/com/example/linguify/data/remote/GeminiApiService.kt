package com.example.linguify.data.remote

import com.example.linguify.BuildConfig
import com.example.linguify.data.remote.model.GeminiRequest
import com.example.linguify.data.remote.model.GeminiResponse
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface GeminiApiService {
    @POST("models/gemini-1.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String = BuildConfig.GEMINI_API_KEY,
        @Body request: GeminiRequest
    ): GeminiResponse
}
