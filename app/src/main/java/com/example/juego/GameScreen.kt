package com.example.juego

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState // ¡NUEVO!
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.juego.data.GameStats
import com.example.juego.data.SaveFormat // ¡NUEVO!
import com.example.juego.ui.theme.GrisFondo

// ¡MODIFICADO! Añadimos el SettingsViewModel
@Composable
fun GameScreen(
    reflexViewModel: ReflexViewModel,
    settingsViewModel: SettingsViewModel
) {
    val uiState by reflexViewModel.uiState.collectAsStateWithLifecycle()
    val stats by reflexViewModel.stats.collectAsStateWithLifecycle()
    // ¡NUEVO! Obtenemos el formato de guardado seleccionado
    val saveFormat by settingsViewModel.saveFormat.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GrisFondo), //
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StatsDisplay(stats = stats)
        // Área de Toque Jugador 1
        TouchArea(
            player = 1,
            backgroundColor = uiState.roundColor.color,
            onPlayerTap = { reflexViewModel.processTouch(1) },
            modifier = Modifier
                .weight(1f) //
                .fillMaxWidth()
        )

        // Área Central (Objetivo y Puntuación)
        TargetDisplay(
            state = uiState
        )

        // Área de Toque Jugador 2
        TouchArea(
            player = 2,
            backgroundColor = uiState.roundColor.color,
            onPlayerTap = { reflexViewModel.processTouch(2) },
            modifier = Modifier
                .weight(1f) //
                .fillMaxWidth()
        )

        // ¡MODIFICADO! Controles dinámicos en lugar de ResetButton
        GameControls(
            state = uiState,
            saveFormat = saveFormat,
            onReset = { reflexViewModel.resetGame() },
            onSave = {
                // Creamos un nombre de archivo único basado en la hora
                val fileName = "partida_${System.currentTimeMillis()}"
                reflexViewModel.saveCurrentGame(fileName, saveFormat)
            },
            onResume = { reflexViewModel.resumeGame() }
        )
    }
}

// ... (StatsDisplay se queda igual) ...
@Composable
fun StatsDisplay(stats: GameStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp, horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "Partidas: ${stats.gamesPlayed}",
            color = Color.White,
            fontSize = 14.sp
        )
        Text(
            text = "J1: ${stats.player1Wins}",
            color = Color.White,
            fontSize = 14.sp
        )
        Text(
            text = "J2: ${stats.player2Wins}",
            color = Color.White,
            fontSize = 14.sp
        )
    }
}

// ... (TouchArea se queda igual) ...
@Composable
fun TouchArea(
    player: Int,
    backgroundColor: Color,
    onPlayerTap: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Animación de color para la transición
    val animatedColor by animateColorAsState(
        targetValue = backgroundColor,
        animationSpec = tween(150), // Transición suave
        label = "TouchAreaColor"
    )

    Box(
        modifier = modifier
            .background(animatedColor)
            .clickable { onPlayerTap(player) }, //
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "JUGADOR $player",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

// ... (TargetDisplay se queda igual, pero añadimos el estado PAUSED) ...
@Composable
fun TargetDisplay(state: GameUiState) {
    // Usar AnimatedContent para transiciones de texto
    AnimatedContent(
        targetState = state.gameState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
        label = "TargetMessage"
    ) { targetState ->
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            when (targetState) {
                GamePhase.GAME_OVER -> {
                    // Mensaje de Victoria
                    Text(
                        text = state.winnerMessage,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                // ¡NUEVO! Mostrar mensaje de pausa
                GamePhase.PAUSED -> {
                    Text(
                        text = "Juego Pausado",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                else -> {
                    // Anuncio de Color Objetivo
                    Text(
                        text = "¡Presiona el ${state.targetColor.nombre}!",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = state.targetColor.color
                    )
                }
            }

            // ¡NUEVO! Mostrar tiempo transcurrido
            Text(
                text = "Tiempo: ${state.timeElapsed}s",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.7f),
                modifier = Modifier.padding(top = 4.dp)
            )

            // Mostrar puntuación
            if (targetState != GamePhase.GAME_OVER) {
                Text(
                    text = "${state.scoreJ1} - ${state.scoreJ2}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }
}


// ¡NUEVO! Composable para los botones de control
@Composable
fun GameControls(
    state: GameUiState,
    saveFormat: SaveFormat,
    onReset: () -> Unit,
    onSave: () -> Unit,
    onResume: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        when (state.gameState) {
            // Si el juego terminó, solo mostrar "Nueva Partida"
            GamePhase.GAME_OVER -> {
                Button(onClick = onReset, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Nueva Partida", fontSize = 18.sp)
                }
            }
            // Si está en pausa, mostrar "Reanudar" y "Reiniciar"
            GamePhase.PAUSED -> {
                Button(onClick = onResume, modifier = Modifier.weight(1f)) {
                    Text(text = "Reanudar", fontSize = 18.sp)
                }
                Spacer(Modifier.width(16.dp))
                Button(onClick = onReset, modifier = Modifier.weight(1f)) {
                    Text(text = "Reiniciar", fontSize = 18.sp)
                }
            }
            // En cualquier otro estado (jugando), mostrar "Guardar" y "Reiniciar"
            else -> { // ESPERA, GO, PROCESANDO
                Button(onClick = onSave, modifier = Modifier.weight(1f)) {
                    Text(text = "Guardar", fontSize = 18.sp)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = onReset, modifier = Modifier.weight(1f)) {
                    Text(text = "Reiniciar", fontSize = 18.sp)
                }
            }
        }
    }
}