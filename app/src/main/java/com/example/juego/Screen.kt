package com.example.juego

// Define las rutas únicas para cada pantalla
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Game : Screen("game")
    object Settings : Screen("settings")

    // --- ¡NUEVAS RUTAS! ---
    object LocalModeSelect : Screen("local_mode_select") // Pantalla para elegir Clásico, Contrarreloj, etc.
    object MultiplayerLobby : Screen("multiplayer_lobby") // Pantalla para Hostear/Unirse a Bluetooth
}