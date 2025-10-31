package com.example.juego

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.juego.data.GameSaveRepository // ¡NUEVO!
import com.example.juego.data.StatsRepository
import com.example.juego.data.ThemeRepository

// Esta fábrica ahora puede crear cualquier ViewModel que necesitemos
class AppViewModelFactory(
    private val statsRepository: StatsRepository,
    private val themeRepository: ThemeRepository, // ¡NUEVO!
    private val gameSaveRepository: GameSaveRepository // ¡NUEVO!
) : ViewModelProvider.Factory {

    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            // Si piden un ReflexViewModel
            modelClass.isAssignableFrom(ReflexViewModel::class.java) -> {
                // ¡MODIFICADO! Pasamos ambos repositorios
                ReflexViewModel(statsRepository, gameSaveRepository) as T
            }
            // Si piden un SettingsViewModel
            modelClass.isAssignableFrom(SettingsViewModel::class.java) -> {
                // ¡MODIFICADO! Pasamos también el gameSaveRepository
                SettingsViewModel(themeRepository, gameSaveRepository) as T
            }
            else -> {
                throw IllegalArgumentException("Unknown ViewModel class")
            }
        }
    }
}