package com.example.guideachat.data.remote

import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit
import okhttp3.logging.HttpLoggingInterceptor

// --- DTOs pour Gemini ---
@Serializable data class GeminiRequest(val contents: List<Content>)
@Serializable data class Content(val parts: List<Part>)
@Serializable data class Part(val text: String)
@Serializable data class GeminiResponse(val candidates: List<Candidate>?)
@Serializable data class Candidate(val content: Content?)

// --- DTOs pour Google Search ---
@Serializable data class SearchResponse(val items: List<SearchItem>?)
@Serializable data class SearchItem(val link: String)

// --- Interfaces API ---
interface GeminiApi {
    @POST("v1beta/models/gemini-2.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

interface GoogleSearchApi {
    @GET("customsearch/v1")
    suspend fun searchImage(
        @Query("key") apiKey: String,
        @Query("cx") cx: String,
        @Query("q") query: String,
        @Query("searchType") searchType: String = "image",
        @Query("num") num: Int = 1
    ): SearchResponse
}

// --- Singleton Réseau ---
object NetworkModule {
    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }
    private val contentType = "application/json".toMediaType()

    // 1. Crée l'intercepteur de logs
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY // Affiche tout : URL, Headers et Réponse
    }

    // 2. Ajoute-le au client OkHttp
    private val client = OkHttpClient.Builder()
        .addInterceptor(loggingInterceptor) // <--- C'est ici que ça se passe
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    // Client pour Gemini (Base URL Google AI)
    val geminiApi: GeminiApi = Retrofit.Builder()
        .baseUrl("https://generativelanguage.googleapis.com/")
        .client(client)
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()
        .create(GeminiApi::class.java)

    // Client pour Google Search
    val googleSearchApi: GoogleSearchApi = Retrofit.Builder()
        .baseUrl("https://www.googleapis.com/")
        .client(client)
        .addConverterFactory(json.asConverterFactory(contentType))
        .build()
        .create(GoogleSearchApi::class.java)

    // (Ajouter Mistral ici plus tard avec la même logique si besoin)
}