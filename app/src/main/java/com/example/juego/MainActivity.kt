package com.example.juego

import android.Manifest // ¡NUEVO!
import android.os.Build // ¡NUEVO!
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts // ¡NUEVO!
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
            application.savedGameMetadataDao
        )
    }

    private val reflexViewModel: ReflexViewModel by viewModels { viewModelFactory }
    private val settingsViewModel: SettingsViewModel by viewModels { viewModelFactory }

    // --- ¡NUEVA LÓGICA DE PERMISOS! ---
    private val requestBluetoothPermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            perms.entries.forEach { (permission, isGranted) ->
                // Aquí puedes manejar si el usuario denegó algún permiso
                // Por ahora, solo lo solicitamos.
            }
        }
    // ---------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // --- ¡NUEVO! Solicitar permisos al crear ---
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            requestBluetoothPermissions.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_ADVERTISE,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        }
        // -----------------------------------------

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
                        }
                    }
                }
            }
        }
    }
}