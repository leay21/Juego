package com.example.juego // Reemplaza con tu package name

import androidx.compose.ui.graphics.Color
import com.example.juego.ui.theme.* // Importa tus colores

// Define los estados posibles del juego
enum class GamePhase {
    ESPERA, // [cite: 6]
    GO,     // [cite: 9]
    PROCESANDO,
    GAME_OVER // [cite: 18]
}

// Modelo de datos para los colores del juego
data class GameColor(
    val nombre: String,
    val color: Color
) {
    companion object {
        val ROJO = GameColor("ROJO", Rojo)
        val AZUL = GameColor("AZUL", Azul)
        val VERDE = GameColor("VERDE", Verde)
        val AMARILLO = GameColor("AMARILLO", Amarillo)
        val NARANJA = GameColor("NARANJA", Naranja)

        fun getRandom(): GameColor {
            return listOf(ROJO, AZUL, VERDE, AMARILLO, NARANJA).random()
        }
    }
}

// Data class que contiene todo el estado de la UI
data class GameUiState(
    val scoreJ1: Int = 0,
    val scoreJ2: Int = 0,
    val gameState: GamePhase = GamePhase.ESPERA,
    val targetColor: GameColor = GameColor.ROJO, // Color objetivo [cite: 7]
    val roundColor: GameColor = GameColor("GRIS", GrisEspera), // Color de la ronda [cite: 10]
    val winnerMessage: String = "" //
)