package com.example.juego

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.juego.data.GameSaveRepository
import com.example.juego.data.SaveFormat
import com.example.juego.data.SavedGameMetadata
import com.example.juego.data.SavedGameMetadataDao
import com.example.juego.data.ThemeRepository
import com.example.juego.ui.theme.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

class SettingsViewModel(
    private val themeRepository: ThemeRepository,
    private val gameSaveRepository: GameSaveRepository,
    private val metadataDao: SavedGameMetadataDao
) : ViewModel() {
    // --- Flow para enviar eventos (Toast) a la UI ---
    private val _toastEvents = MutableSharedFlow<String>()
    val toastEvents: SharedFlow<String> = _toastEvents.asSharedFlow()
    // --------------------------------------------------------

    // --- GESTIÓN DE TEMAS ---
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
    // ¡NUEVA FUNCIÓN!
    /**
     * Cambia el estado 'isFavorite' de una partida guardada.
     */
    fun toggleFavorite(metadata: SavedGameMetadata) {
        viewModelScope.launch {
            // Crea una copia del metadata con el valor de 'isFavorite' invertido
            val updatedMetadata = metadata.copy(isFavorite = !metadata.isFavorite)
            // Llama al DAO para actualizar la entrada en la base de datos
            metadataDao.update(updatedMetadata)
        }
    }
    // -------------------------
    // --- GESTIÓN DE ARCHIVOS ---
    private val _saveFormat = MutableStateFlow(SaveFormat.JSON)
    val saveFormat: StateFlow<SaveFormat> = _saveFormat.asStateFlow()

    fun setSaveFormat(format: SaveFormat) {
        _saveFormat.value = format
    }

    // Obtiene las partidas guardadas directamente de Room
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

    /**
     * Llama al repositorio para exportar un juego y envía un evento Toast.
     */
    fun exportGame(metadata: SavedGameMetadata) {
        viewModelScope.launch {
            val success = gameSaveRepository.exportGameFile(metadata.fileName)
            if (success) {
                _toastEvents.emit("Partida exportada a la carpeta Descargas")
            } else {
                _toastEvents.emit("Error al exportar la partida")
            }
        }
    }

    /**
     * Borra la partida (archivo y metadatos).
     */
    fun deleteGame(metadata: SavedGameMetadata) {
        viewModelScope.launch {
            // 1. Borrar el archivo físico
            gameSaveRepository.deleteGame(metadata.fileName)

            // 2. Borrar la entrada de metadatos en Room
            metadataDao.deleteByFileName(metadata.fileName)
        }
    }

    // ¡CORREGIDO! Las líneas duplicadas de _fileContent se eliminaron de aquí.
}
