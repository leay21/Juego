package com.example.juego // Reemplaza con tu package name

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.juego.ui.theme.JuegoTheme // Reemplaza con el nombre de tu tema

class MainActivity : ComponentActivity() {

    // Inicializa el ViewModel
    private val viewModel: ReflexViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            JuegoTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Muestra la UI leyendo el estado del ViewModel
                    GameScreen(viewModel = viewModel)
                }
            }
        }
    }
}