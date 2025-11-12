package com.example.juego

import android.content.res.Configuration
import android.widget.Toast // ¡NUEVO IMPORT!
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext // ¡NUEVO IMPORT!
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.juego.bt.BluetoothMessage
import com.example.juego.bt.BluetoothViewModel
import com.example.juego.bt.ConnectionState
import com.example.juego.ui.theme.GrisFondo
import com.example.juego.data.GameStats
import com.example.juego.data.SaveFormat
import androidx.compose.foundation.layout.height
import androidx.compose.ui.graphics.graphicsLayer
import androidx.navigation.NavController
import kotlinx.coroutines.flow.collectLatest

@Composable
fun GameScreen(
    navController: NavController,
    reflexViewModel: ReflexViewModel,
    settingsViewModel: SettingsViewModel,
    bluetoothViewModel: BluetoothViewModel
) {
    // --- 1. Determinar el Rol (Host, Client, Local) ---
    val btState by bluetoothViewModel.connectionState.collectAsState()

    val isHost = remember { btState == ConnectionState.LISTENING }
    val isClient = remember { btState == ConnectionState.CONNECTING }
    val isLocal = !isHost && !isClient

    val isGameAuthority = isLocal || isHost

    // --- 2. Obtener el Estado del Juego (GameUiState) ---
    val localUiState by reflexViewModel.uiState.collectAsStateWithLifecycle()
    var clientUiState by remember { mutableStateOf(GameUiState()) }
    val uiState = if (isGameAuthority) localUiState else clientUiState

    // --- 3. Obtener otros estados ---
    val stats by reflexViewModel.stats.collectAsStateWithLifecycle()
    val saveFormat by settingsViewModel.saveFormat.collectAsState()
    val selectedGameMode by bluetoothViewModel.selectedGameMode.collectAsState()

    // ¡NUEVO! Contexto para el Toast
    val context = LocalContext.current

    // --- 4. Lógica de Arranque y Desconexión ---

    // (A) Inicia el juego si somos el Anfitrión
    LaunchedEffect(key1 = isHost) {
        if (isHost) {
            reflexViewModel.startGame(selectedGameMode)
        }
    }

    // (B) ¡NUEVO! Maneja la desconexión
    LaunchedEffect(key1 = btState) {
        // Si el estado (en vivo) vuelve a IDLE, y recordamos que
        // esta pantalla ERA una partida BT (isHost o isClient es true)...
        if (btState == ConnectionState.IDLE && (isHost || isClient)) {
            // ...entonces la conexión se perdió.
            Toast.makeText(context, "Conexión perdida", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
    }

    // --- 5. Lógica de Comunicación Bluetooth ---

    // (A) Anfitrión: Envía el estado al cliente cuando cambia
    LaunchedEffect(key1 = localUiState, key2 = isHost) {
        if (isHost) {
            bluetoothViewModel.sendMessage(BluetoothMessage.GameState(localUiState))
        }
    }

    // (B) Cliente/Anfitrión: Escucha mensajes
    LaunchedEffect(key1 = isGameAuthority) {
        bluetoothViewModel.receivedMessages.collectLatest { message ->
            when(message) {
                is BluetoothMessage.GameState -> {
                    if (isClient) { // Cliente: actualiza su UI
                        clientUiState = message.state
                    }
                }
                is BluetoothMessage.PlayerTouch -> {
                    if (isHost) { // Anfitrión: procesa el toque del cliente
                        reflexViewModel.processTouch(2)
                    }
                }
                else -> {}
            }
        }
    }

    // --- 6. Lógica de Pausa/Salir ---
    DisposableEffect(key1 = Unit) {
        onDispose {
            if (isLocal) {
                reflexViewModel.pauseGame()
            } else {
                // Si salimos de un juego BT, cerramos la conexión
                bluetoothViewModel.closeConnection()
            }
        }
    }

    // --- 7. Detectar Orientación ---
    val orientation = LocalConfiguration.current.orientation

    // --- 8. Elegir y Renderizar el Layout ---
    val onPlayer1Tap = {
        if (isGameAuthority) { // Local o Host
            reflexViewModel.processTouch(1)
        }
        // Cliente no hace nada
    }

    val onPlayer2Tap = {
        if (isLocal) { // Solo Local
            reflexViewModel.processTouch(2)
        } else if (isClient) { // Solo Cliente
            bluetoothViewModel.sendMessage(BluetoothMessage.PlayerTouch())
        }
        // Anfitrión no hace nada (espera el mensaje BT)
    }

    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
        GameScreenLandscape(
            uiState = uiState,
            stats = stats,
            saveFormat = saveFormat,
            onPlayer1Tap = onPlayer1Tap,
            onPlayer2Tap = onPlayer2Tap,
            onReset = { reflexViewModel.resetCurrentGame() },
            onSave = {
                val fileName = "partida_${System.currentTimeMillis()}"
                reflexViewModel.saveCurrentGame(fileName, saveFormat)
            },
            onResume = { reflexViewModel.resumeGame() },
            onPause = { reflexViewModel.pauseGame() },
            onExit = {
                if (!isLocal) bluetoothViewModel.closeConnection()
                navController.popBackStack()
            },
            enableControls = isGameAuthority
        )
    } else {
        GameScreenPortrait(
            uiState = uiState,
            stats = stats,
            saveFormat = saveFormat,
            onPlayer1Tap = onPlayer1Tap,
            onPlayer2Tap = onPlayer2Tap,
            onReset = { reflexViewModel.resetCurrentGame() },
            onSave = {
                val fileName = "partida_${System.currentTimeMillis()}"
                reflexViewModel.saveCurrentGame(fileName, saveFormat)
            },
            onResume = { reflexViewModel.resumeGame() },
            onPause = { reflexViewModel.pauseGame() },
            onExit = {
                if (!isLocal) bluetoothViewModel.closeConnection()
                navController.popBackStack()
            },
            enableControls = isGameAuthority
        )
    }
}

// --- Layout para Modo Vertical (Portrait) ---
@Composable
private fun GameScreenPortrait(
    uiState: GameUiState,
    stats: GameStats,
    saveFormat: SaveFormat,
    onPlayer1Tap: () -> Unit,
    onPlayer2Tap: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    onResume: () -> Unit,
    onPause: () -> Unit,
    onExit: () -> Unit,
    enableControls: Boolean
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
            onPlayerTap = onPlayer1Tap,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
        TargetDisplay(state = uiState)
        TouchArea(
            player = 2,
            backgroundColor = uiState.roundColor.color,
            onPlayerTap = onPlayer2Tap,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
        GameControls(
            state = uiState,
            saveFormat = saveFormat,
            onReset = onReset,
            onSave = onSave,
            onResume = onResume,
            onPause = onPause,
            onExit = onExit,
            enableControls = enableControls
        )
    }
}

// --- Layout para Modo Horizontal (Landscape) ---
@Composable
private fun GameScreenLandscape(
    uiState: GameUiState,
    stats: GameStats,
    saveFormat: SaveFormat,
    onPlayer1Tap: () -> Unit,
    onPlayer2Tap: () -> Unit,
    onReset: () -> Unit,
    onSave: () -> Unit,
    onResume: () -> Unit,
    onPause: () -> Unit,
    onExit: () -> Unit,
    enableControls: Boolean
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
            onPlayerTap = onPlayer1Tap,
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
            GameControls(
                state = uiState,
                saveFormat = saveFormat,
                onReset = onReset,
                onSave = onSave,
                onResume = onResume,
                onPause = onPause,
                onExit = onExit,
                enableControls = enableControls
            )
        }
        TouchArea(
            player = 2,
            backgroundColor = uiState.roundColor.color,
            onPlayerTap = onPlayer2Tap,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
    }
}


// ... (StatsDisplay no cambia) ...
@Composable
fun StatsDisplay(stats: GameStats) {
    // (Tu implementación existente)
}

// ... (TouchArea no cambia) ...
@Composable
fun TouchArea(
    player: Int,
    backgroundColor: Color,
    onPlayerTap: () -> Unit,
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
            .clickable { onPlayerTap() },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "JUGADOR $player",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = if (player == 1) Modifier.graphicsLayer(rotationZ = 180f) else Modifier
        )
    }
}

// ... (TargetDisplay, TargetMessage, y TimeAndScore no cambian) ...
@Composable
fun TargetDisplay(state: GameUiState) {
    // (Tu implementación existente)
}

@Composable
private fun TargetMessage(targetState: GamePhase, state: GameUiState, isRotated: Boolean) {
    // (Tu implementación existente)
}

@Composable
private fun TimeAndScore(state: GameUiState, targetState: GamePhase) {
    // (Tu implementación existente)
}


// --- ¡GameControls MODIFICADO! ---
@Composable
fun GameControls(
    state: GameUiState,
    saveFormat: SaveFormat,
    onReset: () -> Unit,
    onSave: () -> Unit,
    onResume: () -> Unit,
    onPause: () -> Unit,
    onExit: () -> Unit,
    enableControls: Boolean
) {
    val showFullControls = enableControls || state.gameState == GamePhase.GAME_OVER

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (!showFullControls && state.gameState != GamePhase.PAUSED) {
            // Cliente durante el juego
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = onExit, modifier = Modifier.fillMaxWidth()) {
                    Text(text = "Salir", fontSize = 18.sp)
                }
            }
        } else {
            // Lógica de botones para Anfitrión, Local, o Fin de Juego
            when (state.gameState) {
                GamePhase.GAME_OVER -> {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(onClick = onReset, modifier = Modifier.weight(1f), enabled = enableControls) {
                            Text(text = "Nueva Partida", fontSize = 18.sp)
                        }
                        Spacer(Modifier.width(16.dp))
                        Button(onClick = onExit, modifier = Modifier.weight(1f)) {
                            Text(text = "Salir", fontSize = 18.sp)
                        }
                    }
                }
                GamePhase.PAUSED -> {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(onClick = onResume, modifier = Modifier.weight(1f), enabled = enableControls) {
                            Text(text = "Reanudar", fontSize = 18.sp)
                        }
                        Spacer(Modifier.width(16.dp))
                        Button(onClick = onSave, modifier = Modifier.weight(1f), enabled = enableControls) {
                            Text(text = "Guardar", fontSize = 18.sp)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(onClick = onReset, modifier = Modifier.weight(1f), enabled = enableControls) {
                            Text(text = "Reiniciar", fontSize = 18.sp)
                        }
                        Spacer(Modifier.width(16.dp))
                        Button(onClick = onExit, modifier = Modifier.weight(1f)) {
                            Text(text = "Salir", fontSize = 18.sp)
                        }
                    }
                }
                else -> { // JUGANDO (ESPERA, GO, PROCESANDO)
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(onClick = onPause, modifier = Modifier.weight(1f), enabled = enableControls) {
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
}