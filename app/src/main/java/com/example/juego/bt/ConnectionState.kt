package com.example.juego.bt

// Enum para rastrear el estado de la conexión
enum class ConnectionState {
    IDLE,       // Esperando, sin hacer nada
    LISTENING,  // Actuando como Anfitrión, esperando a un Cliente
    CONNECTING, // Actuando como Cliente, intentando conectar
    CONNECTED   // Conexión activa
}