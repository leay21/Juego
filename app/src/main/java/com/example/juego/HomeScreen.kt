package com.example.juego

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState // ¡NUEVO IMPORT!
import androidx.compose.foundation.verticalScroll // ¡NUEVO IMPORT!
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun HomeScreen(
    navController: NavController,
    reflexViewModel: ReflexViewModel = viewModel(factory = LocalViewModelFactory.current)
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()) // ¡LÍNEA AÑADIDA!
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center // Mantiene el centrado si el contenido cabe
        ) {
            Text(
                "Duelo de Reflejos",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Spacer(modifier = Modifier.height(64.dp))

            // Botón Modo Clásico
            Button(
                onClick = {
                    reflexViewModel.startGame(GameMode.CLASSIC)
                    navController.navigate(Screen.Game.route)
                },
                modifier = Modifier.size(width = 250.dp, height = 50.dp)
            ) {
                Text("Modo Clásico", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón Modo Contrarreloj
            Button(
                onClick = {
                    reflexViewModel.startGame(GameMode.TIME_ATTACK)
                    navController.navigate(Screen.Game.route)
                },
                modifier = Modifier.size(width = 250.dp, height = 50.dp)
            ) {
                Text("Modo Contrarreloj", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón Modo Confusión
            Button(
                onClick = {
                    reflexViewModel.startGame(GameMode.CONFUSION)
                    navController.navigate(Screen.Game.route)
                },
                modifier = Modifier.size(width = 250.dp, height = 50.dp)
            ) {
                Text("Modo Confusión", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Botón de Ajustes
            Button(
                onClick = { navController.navigate(Screen.Settings.route) },
                modifier = Modifier.size(width = 250.dp, height = 50.dp)
            ) {
                Text("Ajustes y Partidas Guardadas", fontSize = 18.sp)
            }

            // Añadimos un espacio extra al final para que el scroll se vea mejor
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}