package com.example.linguify.data.remote

import com.example.linguify.data.remote.model.PexelsResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface PexelsApiService {
    @GET("search")
    suspend fun searchPhotos(
        @Header("Authorization") apiKey: String,
        @Query("query") query: String,
        @Query("per_page") perPage: Int = 1,
        @Query("page") page: Int = 1,
        @Query("orientation") orientation: String = "landscape"
    ): PexelsResponse
}
