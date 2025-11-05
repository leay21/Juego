package com.example.juego

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.juego.data.GameSaveRepository
import com.example.juego.data.SoundManager // ¡NUEVO!
import com.example.juego.data.StatsRepository
import com.example.juego.data.ThemeRepository

class AppViewModelFactory(
    private val statsRepository: StatsRepository,
    private val themeRepository: ThemeRepository,
    private val gameSaveRepository: GameSaveRepository,
    private val soundManager: SoundManager // ¡NUEVO!
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            // Si piden un ReflexViewModel
            modelClass.isAssignableFrom(ReflexViewModel::class.java) -> {
                // ¡MODIFICADO! Pasamos el soundManager
                ReflexViewModel(statsRepository, gameSaveRepository, soundManager) as T
            }
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                SettingsViewModel(themeRepository, gameSaveRepository) as T
            }
            else -> {
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}