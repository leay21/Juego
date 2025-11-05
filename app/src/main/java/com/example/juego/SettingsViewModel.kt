package com.example.juego

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.juego.data.GameSaveRepository
import com.example.juego.data.SaveFormat
import com.example.juego.data.SavedGameMetadata // ¡NUEVO!
import com.example.juego.data.SavedGameMetadataDao // ¡NUEVO!
import com.example.juego.data.ThemeRepository
import com.example.juego.ui.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
// ¡ELIMINADO! 'update' ya no se usa
import kotlinx.coroutines.launch

// ¡MODIFICADO! El constructor que me pasaste es correcto
class SettingsViewModel(
    private val themeRepository: ThemeRepository,
    private val gameSaveRepository: GameSaveRepository,
    private val metadataDao: SavedGameMetadataDao // ¡NUEVO!
) : ViewModel() {

    // --- GESTIÓN DE TEMAS --- (Esta parte estaba bien)

    val appTheme: StateFlow<AppTheme> = themeRepository.appTheme.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = AppTheme.SYSTEM
    )

    fun setAppTheme(theme: AppTheme) {
        viewModelScope.launch {
            themeRepository.setAppTheme(theme)
        }
    }

    // --- GESTIÓN DE ARCHIVOS --- (¡TODA ESTA SECCIÓN HA SIDO CORREGIDA!)

    // La preferencia de formato (esto estaba bien)
    private val _saveFormat = MutableStateFlow(SaveFormat.JSON)
    val saveFormat: StateFlow<SaveFormat> = _saveFormat.asStateFlow()

    fun setSaveFormat(format: SaveFormat) {
        _saveFormat.value = format
    }

    // --- ¡AQUÍ ESTÁ EL CAMBIO PRINCIPAL! ---
    // 'savedGames' ya no es un 'MutableStateFlow<List<String>>'.
    // Ahora es un 'StateFlow<List<SavedGameMetadata>>' que se
    // alimenta directamente del Flow de Room.
    val savedGames: StateFlow<List<SavedGameMetadata>> = metadataDao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    // ------------------------------------------

    // ¡ELIMINADO! El 'init' y 'refreshSavedGamesList()' ya no son necesarios
    // porque el 'Flow' de Room (metadataDao.getAll()) actualiza la UI
    // automáticamente cada vez que la base de datos cambia.

    // ¡MODIFICADO! 'deleteGame' ahora recibe 'SavedGameMetadata'
    // y borra tanto el archivo como la entrada en la base de datos.
    fun deleteGame(metadata: SavedGameMetadata) {
        viewModelScope.launch {
            // 1. Borrar el archivo físico
            gameSaveRepository.deleteGame(metadata.fileName)

            // 2. Borrar la entrada de metadatos en Room
            metadataDao.deleteByFileName(metadata.fileName)
        }
    }
}