package com.example.linguify.data.remote.model

import com.google.gson.annotations.SerializedName

data class WordsApiResponse(
    @SerializedName("word")
    val word: String = "",

    @SerializedName("results")
    val results: List<WordResult>? = null,

    @SerializedName("pronunciation")
    val pronunciation: Pronunciation? = null,

    @SerializedName("syllables")
    val syllables: Syllables? = null,

    @SerializedName("frequency")
    val frequency: Float? = null,

    @SerializedName("synonyms")
    val synonyms: List<String>? = null,

    @SerializedName("antonyms")
    val antonyms: List<String>? = null,

    @SerializedName("examples")
    val examples: List<String>? = null
)

data class WordResult(
    @SerializedName("definition")
    val definition: String? = null,

    @SerializedName("partOfSpeech")
    val partOfSpeech: String? = null,

    @SerializedName("synonyms")
    val synonyms: List<String>? = null,

    @SerializedName("antonyms")
    val antonyms: List<String>? = null,

    @SerializedName("examples")
    val examples: List<String>? = null,

    @SerializedName("typeOf")
    val typeOf: List<String>? = null,

    @SerializedName("hasTypes")
    val hasTypes: List<String>? = null,

    @SerializedName("derivation")
    val derivation: List<String>? = null,

    @SerializedName("similarTo")
    val similarTo: List<String>? = null
)

data class Pronunciation(
    @SerializedName("all")
    val all: String? = null,

    @SerializedName("noun")
    val noun: String? = null,

    @SerializedName("verb")
    val verb: String? = null,

    @SerializedName("adjective")
    val adjective: String? = null,

    @SerializedName("adverb")
    val adverb: String? = null
)

data class Syllables(
    @SerializedName("count")
    val count: Int? = null,

    @SerializedName("list")
    val list: List<String>? = null
)