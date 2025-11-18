package com.example.juego

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.juego.bt.BluetoothViewModel
import com.example.juego.bt.MultiplayerLobbyScreen
import com.example.juego.ui.theme.JuegoTheme

val LocalViewModelFactory = staticCompositionLocalOf<ViewModelProvider.Factory> {
    error("No ViewModelFactory provided")
}

class MainActivity : ComponentActivity() {

    // --- 1. Factoría y ViewModels ---
    private val viewModelFactory: AppViewModelFactory by lazy {
        val application = (application as ReflexApplication)
        AppViewModelFactory(
            application.statsRepository,
            application.themeRepository,
            application.gameSaveRepository,
            application.soundManager,
            application.savedGameMetadataDao,
            application.bluetoothConnectionManager
        )
    }

    private val reflexViewModel: ReflexViewModel by viewModels { viewModelFactory }
    private val settingsViewModel: SettingsViewModel by viewModels { viewModelFactory }
    private val bluetoothViewModel: BluetoothViewModel by viewModels { viewModelFactory }

    // --- 2. Lógica de Permisos de Bluetooth/Ubicación ---
    private val requestBluetoothPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            // Aquí puedes añadir lógica si necesitas manejar la respuesta de los permisos
            // Ejemplo: si se denegaron, puedes mostrar un Toast o un diálogo.
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Definición dinámica de los permisos a solicitar
        val permissionsToRequest = mutableListOf(
            // Permisos de ubicación necesarios para Bluetooth LE en general
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
        )

        // Permisos de Bluetooth específicos para Android 12 (API 31) y superiores
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsToRequest.addAll(listOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_CONNECT
            ))
        }

        // Lanzamos la solicitud de permisos al iniciar
        requestBluetoothPermissions.launch(permissionsToRequest.toTypedArray())


        // --- 3. Composable Content ---
        setContent {
            val appTheme by settingsViewModel.appTheme.collectAsState()

            CompositionLocalProvider(LocalViewModelFactory provides viewModelFactory) {
                JuegoTheme(appTheme = appTheme) {

                    val navController = rememberNavController()
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Home.route
                        ) {
                            // Ruta 1: Menú Principal
                            composable(Screen.Home.route) {
                                HomeScreen(navController = navController)
                            }

                            // Ruta 2: Pantalla de Juego
                            // CORREGIDA: Incluye los 4 ViewModels para soportar Local y Bluetooth
                            composable(Screen.Game.route) {
                                GameScreen(
                                    navController = navController,
                                    reflexViewModel = reflexViewModel,
                                    settingsViewModel = settingsViewModel,
                                    bluetoothViewModel = bluetoothViewModel // MANTENIDO
                                )
                            }

                            // Ruta 3: Ajustes
                            composable(Screen.Settings.route) {
                                SettingsScreen(
                                    navController = navController,
                                    settingsViewModel = settingsViewModel,
                                    reflexViewModel = reflexViewModel
                                )
                            }

                            // Ruta 4: Selección de Modo Local
                            composable(Screen.LocalModeSelect.route) {
                                LocalModeScreen(
                                    navController = navController,
                                    reflexViewModel = reflexViewModel
                                )
                            }

                            // Ruta 5: Lobby Multijugador
                            composable(Screen.MultiplayerLobby.route) {
                                MultiplayerLobbyScreen(
                                    navController = navController,
                                    viewModel = bluetoothViewModel
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}