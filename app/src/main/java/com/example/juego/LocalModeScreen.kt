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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalModeScreen(
    navController: NavController,
    reflexViewModel: ReflexViewModel // Recibe el VM para iniciar el juego
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Jugar Local") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                "Selecciona un Modo",
                fontSize = 28.sp,
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
        }
    }
}