package com.example.juego.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_stats")
data class GameStats(
    @PrimaryKey val id: Int = 1, // ID Fijo para tener una sola fila
    val gamesPlayed: Int = 0,
    val player1Wins: Int = 0,
    val player2Wins: Int = 0
) {
    companion object {
        // Objeto por defecto si la BD está vacía
        val default = GameStats(id = 1, 0, 0, 0)
    }
}