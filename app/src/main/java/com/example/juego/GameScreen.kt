package com.example.juego

import android.content.res.Configuration
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.juego.ui.theme.GrisFondo
import com.example.juego.data.GameStats
import com.example.juego.data.SaveFormat
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigation.NavController // ¡NUEVO IMPORT!

// ¡MODIFICADO! Añadido NavController
@Composable
fun GameScreen(
    navController: NavController, // ¡NUEVO!
    reflexViewModel: ReflexViewModel,
    settingsViewModel: SettingsViewModel
) {
    // --- 1. Obtener Estado ---
    val uiState by reflexViewModel.uiState.collectAsStateWithLifecycle()
    val stats by reflexViewModel.stats.collectAsStateWithLifecycle()
    val saveFormat by settingsViewModel.saveFormat.collectAsState()

    // --- 2. Efecto de Pausa (se queda igual) ---
    DisposableEffect(key1 = Unit) {
        onDispose {
            reflexViewModel.pauseGame()
        }
    }

    // --- 3. Detectar Orientación (se queda igual) ---
    val orientation = LocalConfiguration.current.orientation

    // --- 4. Elegir el Layout (¡MODIFICADO! Pasamos nuevos lambdas) ---
    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
        GameScreenLandscape(
            uiState = uiState,
            stats = stats,
            saveFormat = saveFormat,
            onPlayerTap = { reflexViewModel.processTouch(it) },
            onReset = { reflexViewModel.resetCurrentGame() },
            onSave = {
                val fileName = "partida_${System.currentTimeMillis()}"
                reflexViewModel.saveCurrentGame(fileName, saveFormat)
            },
            onResume = { reflexViewModel.resumeGame() },
            onPause = { reflexViewModel.pauseGame() }, // ¡NUEVO!
            onExit = { navController.popBackStack() }   // ¡NUEVO!
        )
    } else {
        GameScreenPortrait(
            uiState = uiState,
            stats = stats,
            saveFormat = saveFormat,
            onPlayerTap = { reflexViewModel.processTouch(it) },
            onReset = { reflexViewModel.resetCurrentGame() },
            onSave = {
                val fileName = "partida_${System.currentTimeMillis()}"
                reflexViewModel.saveCurrentGame(fileName, saveFormat)
            },
            onResume = { reflexViewModel.resumeGame() },
            onPause = { reflexViewModel.pauseGame() }, // ¡NUEVO!
            onExit = { navController.popBackStack() }   // ¡NUEVO!
        )
    }
}

// ¡MODIFICADO! Añadidos onPause y onExit
@Composable
private fun GameScreenPortrait(
    uiState: GameUiState,
    stats: GameStats,
    saveFormat: SaveFormat,
    onPlayerTap: (Int) -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    onResume: () -> Unit,
    onPause: () -> Unit, // ¡NUEVO!
    onExit: () -> Unit   // ¡NUEVO!
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GrisFondo),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StatsDisplay(stats = stats)
        TouchArea(
            player = 1,
            backgroundColor = uiState.roundColor.color,
            onPlayerTap = { onPlayerTap(1) },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
        TargetDisplay(state = uiState)
        TouchArea(
            player = 2,
            backgroundColor = uiState.roundColor.color,
            onPlayerTap = { onPlayerTap(2) },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
        // Pasamos los nuevos controles
        GameControls(
            state = uiState,
            saveFormat = saveFormat,
            onReset = onReset,
            onSave = onSave,
            onResume = onResume,
            onPause = onPause,
            onExit = onExit
        )
    }
}

// ¡MODIFICADO! Añadidos onPause y onExit
@Composable
private fun GameScreenLandscape(
    uiState: GameUiState,
    stats: GameStats,
    saveFormat: SaveFormat,
    onPlayerTap: (Int) -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    onResume: () -> Unit,
    onPause: () -> Unit, // ¡NUEVO!
    onExit: () -> Unit   // ¡NUEVO!
) {
    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(GrisFondo),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TouchArea(
            player = 1,
            backgroundColor = uiState.roundColor.color,
            onPlayerTap = { onPlayerTap(1) },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
        Column(
            modifier = Modifier
                .weight(1.5f)
                .fillMaxHeight()
                .padding(horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            StatsDisplay(stats = stats)
            Spacer(modifier = Modifier.weight(1f))
            TargetDisplay(state = uiState)
            Spacer(modifier = Modifier.weight(1f))
            // Pasamos los nuevos controles
            GameControls(
                state = uiState,
                saveFormat = saveFormat,
                onReset = onReset,
                onSave = onSave,
                onResume = onResume,
                onPause = onPause,
                onExit = onExit
            )
        }
        TouchArea(
            player = 2,
            backgroundColor = uiState.roundColor.color,
            onPlayerTap = { onPlayerTap(2) },
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
    }
}


// ... (StatsDisplay no cambia) ...
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

// ¡MODIFICADO! Se añade la rotación al texto del Jugador 1
@Composable
fun TouchArea(
    player: Int,
    backgroundColor: Color,
    onPlayerTap: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedColor by animateColorAsState(
        targetValue = backgroundColor,
        animationSpec = tween(150),
        label = "TouchAreaColor"
    )

    Box(
        modifier = modifier
            .background(animatedColor)
            .clickable { onPlayerTap(player) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "JUGADOR $player",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            // Rota el texto del Jugador 1
            modifier = if (player == 1) Modifier.graphicsLayer(rotationZ = 180f) else Modifier
        )
    }
}

// ... (TargetDisplay, TargetMessage, y TimeAndScore no cambian) ...
@Composable
fun TargetDisplay(state: GameUiState) {
    AnimatedContent(
        targetState = state.gameState,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        transitionSpec = { fadeIn(tween(300)) togetherWith fadeOut(tween(300)) },
        label = "TargetMessage"
    ) { targetState ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {

            // 1. Texto para Jugador 1 (Arriba) -> ¡ROTADO!
            TargetMessage(targetState, state, isRotated = true)

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Información Central (Tiempo y Puntuación)
            TimeAndScore(state, targetState)

            Spacer(modifier = Modifier.height(16.dp))

            // 3. Texto para Jugador 2 (Abajo) -> ¡NORMAL!
            TargetMessage(targetState, state, isRotated = false)
        }
    }
}

@Composable
private fun TargetMessage(targetState: GamePhase, state: GameUiState, isRotated: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.graphicsLayer(rotationZ = if (isRotated) 180f else 0f)
    ) {
        when (targetState) {
            GamePhase.GAME_OVER -> {
                Text(
                    text = state.winnerMessage,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            GamePhase.PAUSED -> {
                Text(
                    text = "Juego Pausado",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            else -> {
                Text(
                    text = "¡Presiona el ${state.targetColor.nombre}!",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = state.targetTextColor.color
                )
            }
        }
    }
}

@Composable
private fun TimeAndScore(state: GameUiState, targetState: GamePhase) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (state.gameMode == GameMode.TIME_ATTACK) {
            Text(
                text = "Tiempo Restante: ${state.remainingTime}s",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp)
            )
        } else {
            Text(
                text = "Tiempo: ${state.timeElapsed}s",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }

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


// --- ¡GameControls TOTALMENTE MODIFICADO! ---
@Composable
fun GameControls(
    state: GameUiState,
    saveFormat: SaveFormat,
    onReset: () -> Unit,
    onSave: () -> Unit,
    onResume: () -> Unit,
    onPause: () -> Unit, // ¡NUEVO!
    onExit: () -> Unit    // ¡NUEVO!
) {
    // Usamos una Columna para poder poner 2 filas de botones si es necesario
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        when (state.gameState) {
            GamePhase.GAME_OVER -> {
                // Fila 1: Nueva Partida y Salir
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = onReset, modifier = Modifier.weight(1f)) {
                        Text(text = "Nueva Partida", fontSize = 18.sp)
                    }
                    Spacer(Modifier.width(16.dp))
                    Button(onClick = onExit, modifier = Modifier.weight(1f)) {
                        Text(text = "Salir", fontSize = 18.sp)
                    }
                }
            }
            GamePhase.PAUSED -> {
                // Fila 1: Reanudar y Guardar
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = onResume, modifier = Modifier.weight(1f)) {
                        Text(text = "Reanudar", fontSize = 18.sp)
                    }
                    Spacer(Modifier.width(16.dp))
                    Button(onClick = onSave, modifier = Modifier.weight(1f)) {
                        Text(text = "Guardar", fontSize = 18.sp)
                    }
                }
                Spacer(Modifier.height(8.dp))
                // Fila 2: Reiniciar y Salir
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = onReset, modifier = Modifier.weight(1f)) {
                        Text(text = "Reiniciar", fontSize = 18.sp)
                    }
                    Spacer(Modifier.width(16.dp))
                    Button(onClick = onExit, modifier = Modifier.weight(1f)) {
                        Text(text = "Salir", fontSize = 18.sp)
                    }
                }
            }
            else -> { // ESPERA, GO, PROCESANDO (Jugando)
                // Fila 1: Pausa y Salir
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = onPause, modifier = Modifier.weight(1f)) {
                        Text(text = "Pausa", fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = onExit, modifier = Modifier.weight(1f)) {
                        Text(text = "Salir", fontSize = 18.sp)
                    }
                }
            }
        }
    }
}