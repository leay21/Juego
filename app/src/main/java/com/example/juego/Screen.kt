package com.example.juego

// Define las rutas únicas para cada pantalla
sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Game : Screen("game")
    object Settings : Screen("settings")
}