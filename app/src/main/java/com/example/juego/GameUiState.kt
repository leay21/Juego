package com.example.juego

import androidx.compose.ui.graphics.Color
import com.example.juego.ui.theme.* // Importa tus colores

enum class GameMode {
    CLASSIC, // El original, a 5 puntos
    TIME_ATTACK, // Contrarreloj
    CONFUSION // Clásico pero con colores de texto engañosos
}

// Define los estados posibles del juego
enum class GamePhase {
    ESPERA,
    GO,
    PROCESANDO,
    GAME_OVER,
    PAUSED
}

// ... (La clase GameColor no cambia) ...
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

        private val allColors = listOf(ROJO, AZUL, VERDE, AMARILLO, NARANJA)
        private val GRIS = GameColor("GRIS", GrisEspera)

        fun getRandom(): GameColor {
            return allColors.random()
        }

        fun fromName(name: String?): GameColor {
            return allColors.find { it.nombre == name } ?: GRIS
        }
    }
}

// Data class que contiene todo el estado de la UI
data class GameUiState(
    // Campos existentes
    val scoreJ1: Int = 0,
    val scoreJ2: Int = 0,
    val gameState: GamePhase = GamePhase.ESPERA,
    val targetColorName: String = GameColor.ROJO.nombre,
    val roundColorName: String = "GRIS",
    val winnerMessage: String = "",
    val timeElapsed: Long = 0L,
    val moveHistory: List<String> = emptyList(),

    // ¡NUEVOS CAMPOS!
    val gameMode: GameMode = GameMode.CLASSIC,
    val remainingTime: Int = 60, // Para el modo Contrarreloj
    // Para el modo Confusión. Será null en otros modos.
    val targetTextColorName: String? = null
) {
    // --- Propiedades Calculadas ---

    val targetColor: GameColor
        get() = GameColor.fromName(targetColorName)

    val roundColor: GameColor
        get() = GameColor.fromName(roundColorName)

    // ¡NUEVA! Propiedad calculada para el color del texto
    val targetTextColor: GameColor
        // Si hay un color de texto específico (modo confusión), úsalo.
        // Si no, usa el color objetivo normal.
        get() = GameColor.fromName(targetTextColorName ?: targetColorName)

    // El constructor secundario se mantiene igual, no necesita los nuevos campos
    // ya que tienen valores por defecto.
    constructor(
        scoreJ1: Int = 0,
        scoreJ2: Int = 0,
        gameState: GamePhase = GamePhase.ESPERA,
        targetColor: GameColor,
        roundColor: GameColor,
        winnerMessage: String = "",
        timeElapsed: Long = 0L,
        moveHistory: List<String> = emptyList()
    ) : this(
        scoreJ1 = scoreJ1,
        scoreJ2 = scoreJ2,
        gameState = gameState,
        targetColorName = targetColor.nombre,
        roundColorName = roundColor.nombre,
        winnerMessage = winnerMessage,
        timeElapsed = timeElapsed,
        moveHistory = moveHistory
    )
}