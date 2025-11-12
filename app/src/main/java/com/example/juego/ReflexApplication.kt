package com.example.juego

import android.app.Application
import com.example.juego.bt.BluetoothConnectionManager // ¡NUEVO IMPORT!
import com.example.juego.data.AppDatabase
import com.example.juego.data.GameSaveRepository
import com.example.juego.data.StatsRepository
import com.example.juego.data.ThemeRepository
import com.example.juego.data.SoundManager
import kotlinx.coroutines.CoroutineScope // ¡NUEVO IMPORT!
import kotlinx.coroutines.Dispatchers // ¡NUEVO IMPORT!
import kotlinx.coroutines.SupervisorJob // ¡NUEVO IMPORT!

class ReflexApplication : Application() {

    // ¡NUEVO! Un CoroutineScope para toda la aplicación
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Repositorio de Estadísticas (Room)
    private val database by lazy { AppDatabase.getDatabase(this) }
    val statsRepository by lazy { StatsRepository(database.statsDao()) }
    val savedGameMetadataDao by lazy { database.savedGameMetadataDao() }

    // Repositorio de Temas (DataStore)
    val themeRepository by lazy { ThemeRepository(this) }

    // Repositorio de Guardado de Partidas (Archivos)
    val gameSaveRepository by lazy { GameSaveRepository(this) }

    // Repositorio de Sonido
    val soundManager by lazy { SoundManager(this) }

    // ¡NUEVO! Bluetooth Connection Manager
    // Usará el scope de la aplicación para vivir
    val bluetoothConnectionManager by lazy {
        BluetoothConnectionManager(this, applicationScope)
    }
}