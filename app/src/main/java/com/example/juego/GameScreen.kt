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

// ¡MODIFICADO! Añadido BluetoothViewModel
@Composable
fun GameScreen(
    navController: NavController,
    reflexViewModel: ReflexViewModel,
    settingsViewModel: SettingsViewModel,
    bluetoothViewModel: BluetoothViewModel // ¡NUEVO!
) {
    // --- 1. Determinar el Rol (Host, Client, Local) ---
    val btState by bluetoothViewModel.connectionState.collectAsState()

    // Suponemos que somos Anfitrión si estábamos Escuchando,
    // y Cliente si estábamos Conectando.
    // 'remember' asegura que esto no cambie durante la recomposición.
    val isHost = remember { btState == ConnectionState.LISTENING }
    val isClient = remember { btState == ConnectionState.CONNECTING }
    val isLocal = !isHost && !isClient

    // El rol determina si el juego se controla localmente
    val isGameAuthority = isLocal || isHost

    // --- 2. Obtener el Estado del Juego (GameUiState) ---

    // El estado del Host/Local viene del ReflexViewModel
    val localUiState by reflexViewModel.uiState.collectAsStateWithLifecycle()

    // El estado del Cliente viene de un 'remember' que se actualiza por Bluetooth
    var clientUiState by remember { mutableStateOf(GameUiState()) }

    // El estado final que ve la UI
    val uiState = if (isGameAuthority) localUiState else clientUiState

    // --- 3. Obtener otros estados (Stats, Formato de guardado) ---
    val stats by reflexViewModel.stats.collectAsStateWithLifecycle()
    val saveFormat by settingsViewModel.saveFormat.collectAsState()

    // --- 4. Lógica de Comunicación Bluetooth ---
    LaunchedEffect(key1 = localUiState, key2 = isHost) {
        // ANFITRIÓN (Host): Si el estado local cambia, envíalo al cliente.
        if (isHost) {
            bluetoothViewModel.sendMessage(BluetoothMessage.GameState(localUiState))
        }
    }

    LaunchedEffect(key1 = isGameAuthority) {
        // CLIENTE: Escucha por actualizaciones de estado del Anfitrión
        // ANFITRIÓN: Escucha por toques del Cliente
        bluetoothViewModel.receivedMessages.collectLatest { message ->
            when(message) {
                is BluetoothMessage.GameState -> {
                    // Si soy Cliente, actualizo mi estado
                    if (isClient) {
                        clientUiState = message.state
                    }
                }
                is BluetoothMessage.PlayerTouch -> {
                    // Si soy Anfitrión, proceso el toque del Cliente como Jugador 2
                    if (isHost) {
                        reflexViewModel.processTouch(2)
                    }
                }
                // (Manejar otros mensajes como StartGame, ResetGame si los implementamos)
                else -> {}
            }
        }
    }

    // --- 5. Lógica de Pausa/Salir ---
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

    // --- 6. Detectar Orientación ---
    val orientation = LocalConfiguration.current.orientation

    // --- 7. Elegir y Renderizar el Layout ---
    val onPlayer1Tap = {
        if (isGameAuthority) { // Local o Host
            reflexViewModel.processTouch(1)
        }
        // Si soy Cliente, no hago nada al tocar J1
    }

    val onPlayer2Tap = {
        if (isLocal) { // Solo Local
            reflexViewModel.processTouch(2)
        } else if (isClient) { // Solo Cliente
            // Envía el toque al anfitrión
            bluetoothViewModel.sendMessage(BluetoothMessage.PlayerTouch())
        }
        // Si soy Anfitrión, no hago nada (espero el mensaje BT)
    }

    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
        GameScreenLandscape(
            uiState = uiState,
            stats = stats,
            saveFormat = saveFormat,
            onPlayer1Tap = onPlayer1Tap,
            onPlayer2Tap = onPlayer2Tap,
            onReset = { /* TODO: Manejar reinicio BT */ reflexViewModel.resetCurrentGame() },
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
            // Deshabilitar botones si no somos la autoridad (Cliente)
            enableControls = isGameAuthority
        )
    } else {
        GameScreenPortrait(
            uiState = uiState,
            stats = stats,
            saveFormat = saveFormat,
            onPlayer1Tap = onPlayer1Tap,
            onPlayer2Tap = onPlayer2Tap,
            onReset = { /* TODO: Manejar reinicio BT */ reflexViewModel.resetCurrentGame() },
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
            // Deshabilitar botones si no somos la autoridad (Cliente)
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
    enableControls: Boolean // ¡NUEVO!
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
            onPlayerTap = onPlayer1Tap, // Toca J1
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        )
        TargetDisplay(state = uiState)
        TouchArea(
            player = 2,
            backgroundColor = uiState.roundColor.color,
            onPlayerTap = onPlayer2Tap, // Toca J2
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
            enableControls = enableControls // Pasa el estado de habilitado
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
    enableControls: Boolean // ¡NUEVO!
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
            onPlayerTap = onPlayer1Tap, // Toca J1
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
                enableControls = enableControls // Pasa el estado de habilitado
            )
        }
        TouchArea(
            player = 2,
            backgroundColor = uiState.roundColor.color,
            onPlayerTap = onPlayer2Tap, // Toca J2
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        )
    }
}


// ... (StatsDisplay no cambia) ...
@Composable
fun StatsDisplay(stats: GameStats) { /* ... */ }

// ... (TouchArea no cambia) ...
@Composable
fun TouchArea(
    player: Int,
    backgroundColor: Color,
    onPlayerTap: () -> Unit, // Modificado a un lambda simple
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
            .clickable { onPlayerTap() }, // Llama al lambda
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
fun TargetDisplay(state: GameUiState) { /* ... */ }

@Composable
private fun TargetMessage(targetState: GamePhase, state: GameUiState, isRotated: Boolean) { /* ... */ }

@Composable
private fun TimeAndScore(state: GameUiState, targetState: GamePhase) { /* ... */ }


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
    enableControls: Boolean // ¡NUEVO!
) {
    // El cliente (no-autoridad) no puede Pausar, Guardar, Reiniciar o Reanudar.
    // Solo puede Salir.

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