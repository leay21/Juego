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

    val savedGames: StateFlow<List<SavedGameMetadata>> = metadataDao.getAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
    // --- Lógica para visualizar contenido ---
    private val _fileContent = MutableStateFlow<String?>(null)
    val fileContent: StateFlow<String?> = _fileContent.asStateFlow()

    /**
     * Lee el contenido de un archivo y lo pone en el state.
     */
    fun viewFileContent(fileName: String) {
        viewModelScope.launch {
            _fileContent.value = gameSaveRepository.readRawFileContent(fileName)
                ?: "No se pudo leer el archivo." // Mensaje de error si falla
        }
    }

    /**
     * Limpia el contenido del archivo del state (cierra el diálogo).
     */
    fun clearFileContent() {
        _fileContent.value = null
    }

    fun deleteGame(metadata: SavedGameMetadata) {
        viewModelScope.launch {
            // 1. Borrar el archivo físico
            gameSaveRepository.deleteGame(metadata.fileName)

            // 2. Borrar la entrada de metadatos en Room
            metadataDao.deleteByFileName(metadata.fileName)
        }
    }
}