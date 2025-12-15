package com.example.guideachat.data.repository

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.viewModelScope
import com.example.guideachat.data.local.CarDao
import com.example.guideachat.data.model.VoitureEntity
import com.example.guideachat.data.remote.Content
import com.example.guideachat.data.remote.GeminiRequest
import com.example.guideachat.data.remote.NetworkModule
import com.example.guideachat.data.remote.Part
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class CarRepository(private val carDao: CarDao) {

    private val jsonParser = Json { ignoreUnknownKeys = true }

    private val GEMINI_KEY = "use/your/value"
    private val GOOGLE_KEY = "use/your/value"
    private val GOOGLE_CX = "use/your/value"

    suspend fun getVoitureInfo(marque: String, modele: String): Result<VoitureEntity> {
        val id = "${marque}_${modele}".lowercase().replace(" ", "_")

        val cachedCar = carDao.getVoiture(id)
        if (cachedCar != null) {
            Log.d("REPO", "Trouvé en cache local !")
            return Result.success(cachedCar)
        }

        Log.d("REPO", "Pas de cache, appel Gemini...")
        try {
            val prompt = """
                Tu es un expert automobile. Donne-moi les infos sur la "$marque $modele" (version européenne).
                
                IMPORTANT : Pour chaque moteur, donne une estimation de la consommation mixte réaliste (basée sur les données type Spritmonitor).
                
                Réponds UNIQUEMENT avec ce JSON strict :
                {
                  "marque": "$marque",
                  "nom_modele": "$modele (Génération précise)",
                  "annees_production": [2015, 2020], (Mets null si toujours en production)
                  "prix_min": 8000,
                  "prix_max": 15000,
                  "transmission": ["Manuelle", "Automatique"],
                  "bilan": {
                    "fiabilite_texte": "Résumé fiabilité...",
                    "moteurs_conseilles": [
                       { "nom": "2.0 BlueHDi 150", "conso_mixte": "5.8 L/100km" },
                       { "nom": "1.2 PureTech 130", "conso_mixte": "6.5 L/100km" }
                    ],
                    "moteurs_deconseilles": [
                       { "nom": "1.6 THP 156", "conso_mixte": "7.8 L/100km" }
                    ]
                  }
                }
            """.trimIndent()

            val response = NetworkModule.geminiApi.generateContent(
                apiKey = GEMINI_KEY,
                request = GeminiRequest(listOf(Content(listOf(Part(prompt)))))
            )

            val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return Result.failure(Exception("Gemini n'a rien renvoyé"))

            val cleanJson = rawText.replace("```json", "").replace("```", "").trim()
            var voitureData = jsonParser.decodeFromString<VoitureEntity>(cleanJson)

            var photoUrl: String? = null
            try {
                Log.d("REPO", "Recherche image pour : $marque $modele voiture exterieur")

                val searchRes = NetworkModule.googleSearchApi.searchImage(
                    apiKey = GOOGLE_KEY, cx = GOOGLE_CX, query = "$marque $modele voiture exterieur"
                )

                photoUrl = searchRes.items?.firstOrNull()?.link

                Log.d("REPO", "URL Image trouvée : $photoUrl")

            } catch (e: Exception) {
                Log.e("REPO", "Erreur API Image: ${e.message}")
            }

            val finalVoiture = voitureData.copy(
                id_modele = id,
                photo_url = photoUrl
            )

            carDao.insertVoiture(finalVoiture)
            return Result.success(finalVoiture)

        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    suspend fun deleteCar(id: String) {
        carDao.deleteVoiture(id)
    }

    suspend fun clearCache() {
        carDao.clearAll()
    }
}