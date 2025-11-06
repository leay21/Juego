package com.example.juego.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.juego.GameMode

@Entity(tableName = "saved_game_metadata")
data class SavedGameMetadata(
    // El nombre del archivo (ej. "partida_123.json") será nuestra clave primaria
    @PrimaryKey
    val fileName: String,

    val scoreJ1: Int,
    val scoreJ2: Int,
    val gameMode: GameMode,
    val timestamp: Long,
    val isFavorite: Boolean = false // Por defecto, no es favorita
)