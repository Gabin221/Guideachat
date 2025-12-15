package com.example.guideachat.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

// 1. On crée cette nouvelle classe pour stocker Nom + Conso
@Serializable
data class MoteurInfo(
    val nom: String,
    val conso_mixte: String // Ex: "6.5 L/100km"
)

@Serializable
data class BilanFiabilite(
    val fiabilite_texte: String,
    // 2. On change String par MoteurInfo ici
    val moteurs_conseilles: List<MoteurInfo>,
    val moteurs_deconseilles: List<MoteurInfo>
)

@Entity(tableName = "voitures")
@Serializable
data class VoitureEntity(
    @PrimaryKey
    val id_modele: String = "",
    val marque: String,
    val nom_modele: String,
    val annees_production: List<Int?>,
    val prix_min: Int,
    val prix_max: Int,
    val transmission: List<String>,
    val photo_url: String? = null,

    // Pas de changement ici, mais attention à la classe BilanFiabilite juste au dessus
    val bilan: BilanFiabilite,

    val date_maj: Long = System.currentTimeMillis()
)

// On supprime l'ancienne classe MoteursCarburant si elle ne sert plus,
// ou on la laisse si tu l'utilises ailleurs, mais BilanFiabilite est la clé ici.

@Serializable
data class Moteur(
    val nom: String,
    val puissances: List<Int>
)

@Serializable
data class MoteursCarburant(
    val essence: List<Moteur> = emptyList(),
    val diesel: List<Moteur> = emptyList(),
    val electrique: List<Moteur> = emptyList(),
    val hybride: List<Moteur> = emptyList(),
    val flexfuel: List<Moteur> = emptyList(),
    val gpl: List<Moteur> = emptyList()
)

//@Serializable
//data class BilanFiabilite(
//    val fiabilite_texte: String,
//    val moteurs_conseilles: List<String> = emptyList(),
//    val moteurs_deconseilles: List<String> = emptyList(),
//    val moteurs_osef: List<String> = emptyList()
//)
//
//@Entity(tableName = "voitures")
//@Serializable
//data class VoitureEntity(
//    @PrimaryKey
//    val id_modele: String = "",
//
//    val marque: String,
//    val nom_modele: String,
//    val annees_production: List<Int?>,
//    val prix_min: Int,
//    val prix_max: Int,
//    val transmission: List<String>,
//
//    val photo_url: String? = null,
//
//    val moteurs: MoteursCarburant,
//    val bilan: BilanFiabilite,
//
//    val date_maj: Long = System.currentTimeMillis()
//)