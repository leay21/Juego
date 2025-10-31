package com.example.juego.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.juego.ui.theme.AppTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Creamos una instancia de DataStore
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class ThemeRepository(private val context: Context) {

    private val APP_THEME_KEY = stringPreferencesKey("app_theme")

    // Leer el tema guardado (Flow)
    val appTheme: Flow<AppTheme> = context.dataStore.data
        .map { preferences ->
            // Si no hay nada guardado, usa SYSTEM por defecto
            AppTheme.valueOf(preferences[APP_THEME_KEY] ?: AppTheme.SYSTEM.name)
        }

    // Guardar el tema
    suspend fun setAppTheme(theme: AppTheme) {
        context.dataStore.edit { settings ->
            settings[APP_THEME_KEY] = theme.name
        }
    }
}