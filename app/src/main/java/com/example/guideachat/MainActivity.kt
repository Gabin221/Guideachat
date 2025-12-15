package com.example.guideachat

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import com.example.guideachat.data.local.AppDatabase
import com.example.guideachat.data.repository.CarRepository
import com.example.guideachat.databinding.ActivityMainBinding
import com.example.guideachat.ui.CarResultBottomSheet
import com.example.guideachat.ui.MainViewModel
import com.example.guideachat.ui.MainViewModelFactory
import com.example.guideachat.ui.UiState
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = AppDatabase.getDatabase(this)
        val repository = CarRepository(database.carDao())
        val factory = MainViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[MainViewModel::class.java]

        binding.btnSearch.setOnClickListener {
            val query = binding.etSearch.text.toString()
            viewModel.searchCar(query)

            val imm = getSystemService(INPUT_METHOD_SERVICE) as android.view.inputmethod.InputMethodManager
            imm.hideSoftInputFromWindow(binding.root.windowToken, 0)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    //updateUi(state)
                }
            }
        }

        // Dans onCreate

        // ... setup ViewModel ...

        // Bouton Vider Cache (sur la page d'accueil)
        binding.btnClearCache.setOnClickListener {
            viewModel.clearCache()
            Toast.makeText(this, "Mémoire nettoyée", Toast.LENGTH_SHORT).show()
        }

        // Observer l'état
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.visibility = if (state is UiState.Loading) View.VISIBLE else View.GONE

                    if (state is UiState.Success) {
                        // C'EST ICI LE CHANGEMENT : On ouvre la Modale
                        val bottomSheet = CarResultBottomSheet(
                            voiture = state.voiture,
                            onDeleteClicked = { id -> viewModel.deleteCar(id) }
                        )
                        bottomSheet.show(supportFragmentManager, "CarResult")

                        // On reset l'état pour ne pas rouvrir la modale si on tourne l'écran
                        // (Optionnel mais conseillé : créer une fonction resetState dans ViewModel)
                    }

                    if (state is UiState.Error) {
                        Toast.makeText(this@MainActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

//    private fun updateUi(state: UiState) {
//        when (state) {
//            is UiState.Empty -> {
//                binding.progressBar.visibility = View.GONE
//                binding.resultContainer.visibility = View.GONE
//            }
//            is UiState.Loading -> {
//                binding.progressBar.visibility = View.VISIBLE
//                binding.resultContainer.visibility = View.GONE
//            }
//            is UiState.Success -> {
//                binding.progressBar.visibility = View.GONE
//                binding.resultContainer.visibility = View.VISIBLE
//
//                val v = state.voiture
//
//                binding.tvMode.text = "${v.marque} ${v.nom_modele}"
//                binding.tvPriceRange.text = "Prix : ${v.prix_min}€ - ${v.prix_max}€"
//                binding.tvProductionYears.text = "Production : ${v.annees_production.firstOrNull()} - ${v.annees_production.lastOrNull()}"
//
//                binding.tvReliabilityText.text = v.bilan.fiabilite_texte
//
//                binding.tvEnginesGood.text = v.bilan.moteurs_conseilles.joinToString("\n") { "- $it" }
//                binding.tvEnginesBad.text = v.bilan.moteurs_deconseilles.joinToString("\n") { "- $it" }
//
//                binding.imgCar.load(v.photo_url) {
//                    crossfade(true)
//                    error(android.R.drawable.ic_menu_report_image) // Image par défaut si erreur
//                }
//            }
//            is UiState.Error -> {
//                binding.progressBar.visibility = View.GONE
//                Toast.makeText(this, "Erreur : ${state.message}", Toast.LENGTH_LONG).show()
//            }
//        }
//    }
}