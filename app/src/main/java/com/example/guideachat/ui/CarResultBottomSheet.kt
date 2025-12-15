package com.example.guideachat.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import coil.load
import com.example.guideachat.data.model.VoitureEntity
import com.example.guideachat.databinding.LayoutCarResultBinding
import com.example.guideachat.utils.PdfUtil
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CarResultBottomSheet(
    private val voiture: VoitureEntity,
    private val onDeleteClicked: (String) -> Unit // Callback pour dire à l'activity de supprimer
) : BottomSheetDialogFragment() {

    private lateinit var binding: LayoutCarResultBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = LayoutCarResultBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Remplissage des données
        binding.tvModelTitle.text = "${voiture.marque} ${voiture.nom_modele}"
        binding.tvPrice.text = "Prix occasion : ${voiture.prix_min}€ - ${voiture.prix_max}€"
        binding.tvReliability.text = voiture.bilan.fiabilite_texte

        binding.tvEnginesGood.text = voiture.bilan.moteurs_conseilles.joinToString("\n") {
            "✅ ${it.nom} (${it.conso_mixte})"
        }

        binding.tvEnginesBad.text = voiture.bilan.moteurs_deconseilles.joinToString("\n") {
            "❌ ${it.nom} (${it.conso_mixte})"
        }

        val anneeDebut = voiture.annees_production.firstOrNull()
        val anneeFin = voiture.annees_production.getOrNull(1) // Peut être null
        val finTexte = anneeFin?.toString() ?: "Aujourd'hui" // Si null -> "Aujourd'hui"
        binding.tvProductionYears.text = "Production : $anneeDebut - $finTexte"

        binding.tvEnginesGood.text = voiture.bilan.moteurs_conseilles.joinToString("\n") { "- ${it.nom} (${it.conso_mixte})" }
        binding.tvEnginesBad.text = voiture.bilan.moteurs_deconseilles.joinToString("\n") { "- ${it.nom} (${it.conso_mixte})" }

        binding.imgCarResult.load(voiture.photo_url) {
            crossfade(true)
            error(android.R.drawable.ic_menu_report_image)
        }

        // Action PDF
        binding.btnSharePdf.setOnClickListener {
            try {
                PdfUtil.generateAndSharePdf(requireContext(), voiture)
            } catch (e: Exception) {
                Toast.makeText(context, "Erreur PDF: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Action Supprimer
        binding.btnDeleteCar.setOnClickListener {
            onDeleteClicked(voiture.id_modele)
            dismiss() // Ferme la modale
        }
    }
}