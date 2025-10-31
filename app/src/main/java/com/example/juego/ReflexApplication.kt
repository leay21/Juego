package com.example.juego

import android.app.Application
import com.example.juego.data.AppDatabase
import com.example.juego.data.StatsRepository

class ReflexApplication : Application() {

    // 'lazy' crea la BD y el repo solo cuando se necesitan
    private val database by lazy { AppDatabase.getDatabase(this) }
    val statsRepository by lazy { StatsRepository(database.statsDao()) }
}