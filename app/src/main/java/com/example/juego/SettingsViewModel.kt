package com.example.juego

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.juego.data.GameSaveRepository // ¡NUEVO!
import com.example.juego.data.SaveFormat // ¡NUEVO!
import com.example.juego.data.ThemeRepository
import com.example.juego.ui.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val themeRepository: ThemeRepository,
    private val gameSaveRepository: GameSaveRepository // ¡NUEVO!
) : ViewModel() {

    // --- GESTIÓN DE TEMAS ---

    // Expone el tema actual a la UI
    val appTheme: StateFlow<AppTheme> = themeRepository.appTheme.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppTheme.SYSTEM // Valor inicial
    )

    // Función para que la UI cambie el tema
    fun setAppTheme(theme: AppTheme) {
        viewModelScope.launch {
            themeRepository.setAppTheme(theme)
        }
    }

    // --- GESTIÓN DE ARCHIVOS --- (¡NUEVO!)

    // TODO: Deberías guardar esta preferencia en DataStore,
    // por ahora la mantenemos en el ViewModel.
    private val _saveFormat = MutableStateFlow(SaveFormat.JSON)
    val saveFormat: StateFlow<SaveFormat> = _saveFormat.asStateFlow()

    private val _savedGames = MutableStateFlow<List<String>>(emptyList())
    val savedGames: StateFlow<List<String>> = _savedGames.asStateFlow()

    init {
        // Carga la lista de partidas guardadas al iniciar
        refreshSavedGamesList()
    }

    fun setSaveFormat(format: SaveFormat) {
        _saveFormat.value = format
    }

    fun refreshSavedGamesList() {
        _savedGames.value = gameSaveRepository.listSavedGames()
    }

    fun deleteGame(fileName: String) {
        viewModelScope.launch {
            gameSaveRepository.deleteGame(fileName)
            refreshSavedGamesList() // Actualiza la lista
        }
    }
}