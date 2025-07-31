package com.example.linguify.data.remote

import com.example.linguify.data.remote.model.WordsApiResponse
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Path

interface WordsApiService {

    @GET("words/{word}")
    suspend fun getWordDetails(
        @Path("word") word: String,
        @Header("X-RapidAPI-Key") apiKey: String,
        @Header("X-RapidAPI-Host") apiHost: String
    ): WordsApiResponse

    @GET("words/{word}/synonyms")
    suspend fun getSynonyms(
        @Path("word") word: String,
        @Header("X-RapidAPI-Key") apiKey: String,
        @Header("X-RapidAPI-Host") apiHost: String
    ): WordsApiResponse

    @GET("words/{word}/antonyms")
    suspend fun getAntonyms(
        @Path("word") word: String,
        @Header("X-RapidAPI-Key") apiKey: String,
        @Header("X-RapidAPI-Host") apiHost: String
    ): WordsApiResponse

    @GET("words/{word}/examples")
    suspend fun getExamples(
        @Path("word") word: String,
        @Header("X-RapidAPI-Key") apiKey: String,
        @Header("X-RapidAPI-Host") apiHost: String
    ): WordsApiResponse

    @GET("words/{word}/partOfSpeech")
    suspend fun getPartOfSpeech(
        @Path("word") word: String,
        @Header("X-RapidAPI-Key") apiKey: String,
        @Header("X-RapidAPI-Host") apiHost: String
    ): WordsApiResponse

    @GET("words/{word}/pronunciation")
    suspend fun getPronunciation(
        @Path("word") word: String,
        @Header("X-RapidAPI-Key") apiKey: String,
        @Header("X-RapidAPI-Host") apiHost: String
    ): WordsApiResponse
}
