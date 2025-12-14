package com.example.guideachat.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.guideachat.data.model.VoitureEntity
import com.example.guideachat.data.repository.CarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainViewModel(private val repository: CarRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Empty)
    val uiState: StateFlow<UiState> = _uiState

    fun searchCar(query: String) {
        if (query.isBlank()) return

        _uiState.value = UiState.Loading

        viewModelScope.launch {
            val parts = query.trim().split(" ", limit = 2)
            val marque = parts.getOrElse(0) { "" }
            val modele = parts.getOrElse(1) { "" }

            val result = repository.getVoitureInfo(marque, modele)

            if (result.isSuccess) {
                _uiState.value = UiState.Success(result.getOrNull()!!)
            } else {
                _uiState.value = UiState.Error(result.exceptionOrNull()?.message ?: "Erreur inconnue")
            }
        }
    }
}

sealed class UiState {
    object Empty : UiState()
    object Loading : UiState()
    data class Success(val voiture: VoitureEntity) : UiState()
    data class Error(val message: String) : UiState()
}

class MainViewModelFactory(private val repository: CarRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}