package com.example.juego

import android.app.Application
import com.example.juego.data.AppDatabase
import com.example.juego.data.StatsRepository
import com.example.juego.data.ThemeRepository // ¡NUEVO!

class ReflexApplication : Application() {

    // Repositorio de Estadísticas (Room)
    private val database by lazy { AppDatabase.getDatabase(this) }
    val statsRepository by lazy { StatsRepository(database.statsDao()) }

    // ¡NUEVO! Repositorio de Temas (DataStore)
    val themeRepository by lazy { ThemeRepository(this) }
}