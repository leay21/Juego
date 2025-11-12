package com.example.juego

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
// ¡ELIMINADO! Ya no necesitamos el ViewModel aquí
// import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

// ¡MODIFICADO! Ya no recibe el ReflexViewModel
@Composable
fun HomeScreen(
    navController: NavController
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Duelo de Reflejos",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(64.dp))

            // --- ¡BOTONES MODIFICADOS! ---

            // Botón Jugar Local
            Button(
                onClick = {
                    // Navega a la nueva pantalla de selección de modo
                    navController.navigate(Screen.LocalModeSelect.route)
                },
                modifier = Modifier.size(width = 280.dp, height = 50.dp)
            ) {
                Text("Jugar Local (1 Dispositivo)", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón Multijugador
            Button(
                onClick = {
                    // Navega a la nueva pantalla de lobby de Bluetooth
                    navController.navigate(Screen.MultiplayerLobby.route)
                },
                modifier = Modifier.size(width = 280.dp, height = 50.dp)
            ) {
                Text("Multijugador (Bluetooth)", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Botón de Ajustes (sin cambios)
            Button(
                onClick = { navController.navigate(Screen.Settings.route) },
                modifier = Modifier.size(width = 280.dp, height = 50.dp)
            ) {
                Text("Ajustes y Partidas Guardadas", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}