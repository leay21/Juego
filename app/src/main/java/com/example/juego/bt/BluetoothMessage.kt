package com.example.juego.bt

import com.example.juego.GameUiState
import com.example.juego.GameMode

// 1. Definimos los tipos de mensaje posibles
enum class MessageType {
    GAME_STATE,
    PLAYER_TOUCH,
    START_GAME,
    RESET_GAME
}

// 2. Creamos una ÚNICA clase de datos que puede llevar cualquier carga
data class BluetoothMessage(
    val type: MessageType,
    // Estos campos son opcionales (null) dependiendo del tipo de mensaje
    val gameState: GameUiState? = null,     // Para cuando el Host envía el estado
    val touchTimestamp: Long? = null,       // Para cuando el Cliente toca la pantalla
    val gameMode: GameMode? = null          // Por si quieres enviar el modo de juego
)