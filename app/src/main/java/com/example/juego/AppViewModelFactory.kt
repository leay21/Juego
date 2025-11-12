package com.example.juego

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.juego.bt.BluetoothConnectionManager // ¡NUEVO IMPORT!
import com.example.juego.bt.BluetoothViewModel // ¡NUEVO IMPORT!
import com.example.juego.data.GameSaveRepository
import com.example.juego.data.SoundManager
import com.example.juego.data.StatsRepository
import com.example.juego.data.ThemeRepository
import com.example.juego.data.SavedGameMetadataDao

// ¡MODIFICADO! Añadido bluetoothConnectionManager al constructor
class AppViewModelFactory(
    private val statsRepository: StatsRepository,
    private val themeRepository: ThemeRepository,
    private val gameSaveRepository: GameSaveRepository,
    private val soundManager: SoundManager,
    private val savedGameMetadataDao: SavedGameMetadataDao,
    private val bluetoothConnectionManager: BluetoothConnectionManager // ¡NUEVO!
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            // Si piden un ReflexViewModel
            modelClass.isAssignableFrom(ReflexViewModel::class.java) -> {
                ReflexViewModel(statsRepository, gameSaveRepository, soundManager, savedGameMetadataDao) as T
            }
            // Si piden un SettingsViewModel
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(themeRepository, gameSaveRepository, savedGameMetadataDao) as T
            }
            // --- ¡NUEVO BLOQUE! ---
            // Si piden un BluetoothViewModel
            modelClass.isAssignableFrom(BluetoothViewModel::class.java) -> {
                BluetoothViewModel(bluetoothConnectionManager) as T
            }
            // ---------------------
            else -> {
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}