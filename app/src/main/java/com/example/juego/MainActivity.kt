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
import com.example.juego.bt.BluetoothViewModel // ¡NUEVO IMPORT!
import com.example.juego.ui.theme.JuegoTheme

val LocalViewModelFactory = staticCompositionLocalOf<ViewModelProvider.Factory> {
    error("No ViewModelFactory provided")
}
class MainActivity : ComponentActivity() {

    private val viewModelFactory: AppViewModelFactory by lazy {
        val application = (application as ReflexApplication)
        // ¡MODIFICADO! Pasamos el nuevo manager
        AppViewModelFactory(
            application.statsRepository,
            application.themeRepository,
            application.gameSaveRepository,
            application.soundManager,
            application.savedGameMetadataDao,
            application.bluetoothConnectionManager // ¡NUEVO!
        )
    }

    // Pedimos los ViewModels usando la fábrica
    private val reflexViewModel: ReflexViewModel by viewModels { viewModelFactory }
    private val settingsViewModel: SettingsViewModel by viewModels { viewModelFactory }
    // ¡NUEVO! Creamos la instancia del BluetoothViewModel
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
                        NavHost(
                            navController = navController,
                            startDestination = Screen.Home.route
                        ) {
                            composable(Screen.Home.route) {
                                HomeScreen(
                                    navController = navController,
                                    reflexViewModel = reflexViewModel
                                )
                            }
                            composable(Screen.Game.route) {
                                GameScreen(
                                    navController = navController,
                                    reflexViewModel = reflexViewModel,
                                    settingsViewModel = settingsViewModel
                                )
                            }
                            composable(Screen.Settings.route) {
                                SettingsScreen(
                                    navController = navController,
                                    settingsViewModel = settingsViewModel,
                                    reflexViewModel = reflexViewModel
                                )
                            }
                            // Próximamente añadiremos aquí las nuevas rutas
                            // para la UI de multijugador, que usarán el
                            // 'bluetoothViewModel'
                        }
                    }
                }
            }
        }
    }
}