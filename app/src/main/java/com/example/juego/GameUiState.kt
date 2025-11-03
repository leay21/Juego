package com.example.juego // Reemplaza con tu package name

import androidx.compose.ui.graphics.Color
import com.example.juego.ui.theme.* // Importa tus colores

// Define los estados posibles del juego
enum class GamePhase {
    ESPERA, //
    GO,     //
    PROCESANDO,
    GAME_OVER, //
    PAUSED // ¡NUEVO! Estado para cuando guardemos
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

        // ¡MODIFICADO! Lista privada para búsquedas
        private val allColors = listOf(ROJO, AZUL, VERDE, AMARILLO, NARANJA)
        private val GRIS = GameColor("GRIS", GrisEspera)

        fun getRandom(): GameColor {
            return allColors.random()
        }

        // ¡NUEVO! Función para obtener un color por su nombre
        fun fromName(name: String?): GameColor {
            return allColors.find { it.nombre == name } ?: GRIS
        }
    }
}

// Data class que contiene todo el estado de la UI
// ¡MODIFICADO! Ahora contiene campos serializables
data class GameUiState(
    val scoreJ1: Int = 0,
    val scoreJ2: Int = 0,
    val gameState: GamePhase = GamePhase.ESPERA,

    // ¡MODIFICADO! Guardamos solo los nombres (Strings)
    // Gson puede guardar Strings, pero no el tipo "Color" de Compose
    val targetColorName: String = GameColor.ROJO.nombre,
    val roundColorName: String = "GRIS",

    val winnerMessage: String = "",

    // ¡NUEVO! Campos requeridos por la práctica
    val timeElapsed: Long = 0L,
    val moveHistory: List<String> = emptyList()
) {
    // --- Propiedades Calculadas ---
    // Gson ignorará estas propiedades (por empezar con 'get'),
    // pero nuestra UI puede usarlas como antes.

    val targetColor: GameColor
        get() = GameColor.fromName(targetColorName)

    val roundColor: GameColor
        get() = GameColor.fromName(roundColorName)

    // ¡NUEVO! Constructor secundario
    // Útil en el ViewModel para crear el estado
    // a partir de los objetos GameColor.
    constructor(
        scoreJ1: Int = 0,
        scoreJ2: Int = 0,
        gameState: GamePhase = GamePhase.ESPERA,
        targetColor: GameColor, // Acepta el objeto
        roundColor: GameColor, // Acepta el objeto
        winnerMessage: String = "",
        timeElapsed: Long = 0L,
        moveHistory: List<String> = emptyList()
    ) : this(
        scoreJ1 = scoreJ1,
        scoreJ2 = scoreJ2,
        gameState = gameState,
        targetColorName = targetColor.nombre, // Guarda solo el String
        roundColorName = roundColor.nombre, // Guarda solo el String
        winnerMessage = winnerMessage,
        timeElapsed = timeElapsed,
        moveHistory = moveHistory
    )
}