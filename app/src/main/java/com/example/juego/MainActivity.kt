package com.example.juego

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.juego.ui.theme.JuegoTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.ViewModelProvider

// Creamos un CompositionLocal para nuestra fábrica
val LocalViewModelFactory = staticCompositionLocalOf<ViewModelProvider.Factory> {
    error("No ViewModelFactory provided")
}
class MainActivity : ComponentActivity() {
    // Creamos la fábrica una vez
    private val viewModelFactory: AppViewModelFactory by lazy {
        val application = (application as ReflexApplication)
        // Pasamos el nuevo repositorio a la fábrica
        AppViewModelFactory(
            application.statsRepository,
            application.themeRepository,
            application.gameSaveRepository,
            application.soundManager,
            application.savedGameMetadataDao
        )
    }

    // Pedimos los ViewModels usando la fábrica
    private val reflexViewModel: ReflexViewModel by viewModels { viewModelFactory }
    private val settingsViewModel: SettingsViewModel by viewModels { viewModelFactory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Leemos el tema desde el SettingsViewModel
            val appTheme by settingsViewModel.appTheme.collectAsState()

            // El tema ahora es dinámico y envuelve toda la app
            CompositionLocalProvider(LocalViewModelFactory provides viewModelFactory) {
                JuegoTheme(appTheme = appTheme) {

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
                                // Pasamos el viewModel explícitamente
                                HomeScreen(
                                    navController = navController,
                                    reflexViewModel = reflexViewModel
                                )
                            }

                            // --- ¡MODIFICACIÓN AQUÍ! ---
                            // Ruta 2: Pantalla del Juego (Game)
                            composable(Screen.Game.route) {
                                // ¡MODIFICADO! Pasamos ambos ViewModels Y el NavController
                                GameScreen(
                                    navController = navController, // <-- AÑADIDO
                                    reflexViewModel = reflexViewModel,
                                    settingsViewModel = settingsViewModel
                                )
                            }
                            // --- FIN DE LA MODIFICACIÓN ---

                            // Ruta 3: Ajustes
                            composable(Screen.Settings.route) {
                                // ¡MODIFICADO! Pasamos ambos ViewModels
                                SettingsScreen(
                                    navController = navController,
                                    settingsViewModel = settingsViewModel,
                                    reflexViewModel = reflexViewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}