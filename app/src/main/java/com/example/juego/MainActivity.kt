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
import com.example.juego.bt.MultiplayerLobbyScreen // ¡NUEVO IMPORT!
import com.example.juego.ui.theme.JuegoTheme

val LocalViewModelFactory = staticCompositionLocalOf<ViewModelProvider.Factory> {
    error("No ViewModelFactory provided")
}
class MainActivity : ComponentActivity() {

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

    // Pedimos los ViewModels usando la fábrica
    private val reflexViewModel: ReflexViewModel by viewModels { viewModelFactory }
    private val settingsViewModel: SettingsViewModel by viewModels { viewModelFactory }
    private val bluetoothViewModel: BluetoothViewModel by viewModels { viewModelFactory }


    private val requestBluetoothPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            // ... (lógica de permisos)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestBluetoothPermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        }

        setContent {
            val appTheme by settingsViewModel.appTheme.collectAsState()

            CompositionLocalProvider(LocalViewModelFactory provides viewModelFactory) {
                JuegoTheme(appTheme = appTheme) {

                    val navController = rememberNavController()
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        // --- ¡NAVHOST MODIFICADO! ---
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Home.route
                        ) {
                            // Ruta 1: Menú Principal (Modificada)
                            composable(Screen.Home.route) {
                                HomeScreen(
                                    navController = navController
                                    // Ya no necesita el VM
                                )
                            }

                            // Ruta 2: Pantalla de Juego (Sin cambios)
                            composable(Screen.Game.route) {
                                GameScreen(
                                    navController = navController,
                                    reflexViewModel = reflexViewModel,
                                    settingsViewModel = settingsViewModel,
                                    bluetoothViewModel = bluetoothViewModel
                                )
                            }

                            // Ruta 3: Ajustes (Sin cambios)
                            composable(Screen.Settings.route) {
                                SettingsScreen(
                                    navController = navController,
                                    settingsViewModel = settingsViewModel,
                                    reflexViewModel = reflexViewModel
                                )
                            }

                            // --- ¡NUEVA RUTA 4! ---
                            composable(Screen.LocalModeSelect.route) {
                                LocalModeScreen(
                                    navController = navController,
                                    reflexViewModel = reflexViewModel
                                )
                            }

                            // --- ¡NUEVA RUTA 5! ---
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