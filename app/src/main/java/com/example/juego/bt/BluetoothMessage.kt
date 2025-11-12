package com.example.juego.bt

import com.example.juego.GameUiState

// Define los mensajes que se pueden enviar por Bluetooth
sealed class BluetoothMessage {

    // Mensaje para enviar el estado completo del juego (Anfitrión -> Cliente)
    data class GameState(val state: GameUiState) : BluetoothMessage()

    // Mensaje para un evento de toque (Cliente -> Anfitrión)
    // El anfitrión sabe que quien envía esto es el Jugador 2
    data class PlayerTouch(val timestamp: Long = System.currentTimeMillis()) : BluetoothMessage()

    // Mensaje para iniciar el juego
    data class StartGame(val mode: com.example.juego.GameMode) : BluetoothMessage()

    // Mensaje para reiniciar el juego
    object ResetGame : BluetoothMessage()
}