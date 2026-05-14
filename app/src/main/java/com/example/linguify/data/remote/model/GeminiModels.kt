package com.example.linguify.data.remote.model

import com.google.gson.annotations.SerializedName

data class GeminiRequest(
    val contents: List<Content>
)

data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String
)

data class GeminiResponse(
    val candidates: List<Candidate>? = null
)

data class Candidate(
    val content: Content,
    @SerializedName("finishReason") val finishReason: String?
)