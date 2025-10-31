package com.example.juego

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.juego.data.ThemeRepository
import com.example.juego.ui.theme.AppTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(private val themeRepository: ThemeRepository) : ViewModel() {

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
}