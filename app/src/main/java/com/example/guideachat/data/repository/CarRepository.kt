package com.example.guideachat.data.repository

import android.util.Log
import com.example.guideachat.data.local.CarDao
import com.example.guideachat.data.model.VoitureEntity
import com.example.guideachat.data.remote.Content
import com.example.guideachat.data.remote.GeminiRequest
import com.example.guideachat.data.remote.NetworkModule
import com.example.guideachat.data.remote.Part
import kotlinx.serialization.json.Json

class CarRepository(private val carDao: CarDao) {

    private val jsonParser = Json { ignoreUnknownKeys = true }

    // Remplacer par tes vraies clés (idéalement dans local.properties/BuildConfig)
    private val GEMINI_KEY = "use/your/value"
    private val GOOGLE_KEY = "use/your/value"
    private val GOOGLE_CX = "use/your/value"

    suspend fun getVoitureInfo(marque: String, modele: String): Result<VoitureEntity> {
        val id = "${marque}_${modele}".lowercase().replace(" ", "_")

        // 1. Vérifier le Cache local
        val cachedCar = carDao.getVoiture(id)
        if (cachedCar != null) {
            Log.d("REPO", "Trouvé en cache local !")
            return Result.success(cachedCar)
        }

        // 2. Si pas en cache, appeler Gemini
        Log.d("REPO", "Pas de cache, appel Gemini...")
        try {
            val prompt = """
                Agis comme un expert automobile. Génère un JSON strict pour la voiture : $marque $modele.
                Respecte EXACTEMENT ce schéma JSON (pas de markdown, juste le json) :
                {
                  "marque": "$marque", "nom_modele": "$modele",
                  "annees_production": [debut, fin], "prix_min": 1000, "prix_max": 5000,
                  "transmission": ["manuelle"],
                  "moteurs": { 
                     "essence": [{"nom": "...", "puissances": [100]}],
                     "diesel": [], "electrique": [], "hybride": [], "flexfuel": [], "gpl": []
                  },
                  "bilan": {
                     "fiabilite_texte": "Résumé fiabilité...",
                     "moteurs_conseilles": ["..."], "moteurs_deconseilles": ["..."], "moteurs_osef": ["..."]
                  }
                }
            """.trimIndent()

            val response = NetworkModule.geminiApi.generateContent(
                apiKey = GEMINI_KEY,
                request = GeminiRequest(listOf(Content(listOf(Part(prompt)))))
            )

            val rawText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                ?: return Result.failure(Exception("Gemini n'a rien renvoyé"))

            // Nettoyage du JSON (Gemini met souvent des ```json au début)
            val cleanJson = rawText.replace("```json", "").replace("```", "").trim()
            var voitureData = jsonParser.decodeFromString<VoitureEntity>(cleanJson)

            // 3. TODO : Appel Mistral pour vérifier (double check) ici
            // voitureData = verifyWithMistral(voitureData)

            // 4. Récupérer l'image
            var photoUrl: String? = null
            try {
                // AJOUTE CE LOG
                Log.d("REPO", "Recherche image pour : $marque $modele voiture exterieur")

                val searchRes = NetworkModule.googleSearchApi.searchImage(
                    apiKey = GOOGLE_KEY, cx = GOOGLE_CX, query = "$marque $modele voiture exterieur"
                )

                photoUrl = searchRes.items?.firstOrNull()?.link

                // AJOUTE CE LOG
                Log.d("REPO", "URL Image trouvée : $photoUrl")

            } catch (e: Exception) {
                Log.e("REPO", "Erreur API Image: ${e.message}")
            }

            // 5. Finaliser l'objet et sauvegarder
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
}