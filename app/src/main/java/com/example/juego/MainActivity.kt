package com.example.juego

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.juego.ui.theme.AppTheme
import com.example.juego.ui.theme.JuegoTheme

class MainActivity : ComponentActivity() {

    // El ViewModel del juego, propiedad de la Activity
    private val viewModel: ReflexViewModel by viewModels {
        ReflexViewModelFactory(
            (application as ReflexApplication).statsRepository
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // NOTA: Por ahora, el tema está FIJO en "SYSTEM".
            // En el siguiente paso, lo haremos dinámico.
            JuegoTheme(appTheme = AppTheme.SYSTEM) {

                val navController = rememberNavController()

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route // Empezamos en el Menú
                    ) {
                        // Ruta 1: Pantalla Principal (Home)
                        composable(Screen.Home.route) {
                            HomeScreen(navController = navController)
                        }

                        // Ruta 2: Pantalla del Juego (Game)
                        composable(Screen.Game.route) {
                            GameScreen(viewModel = viewModel)
                        }

                        // Ruta 3: Ajustes (la crearemos después)
                        // composable(Screen.Settings.route) {
                        //     SettingsScreen()
                        // }
                    }
                }
            }
        }
    }
}