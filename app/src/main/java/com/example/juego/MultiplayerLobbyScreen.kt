package com.example.juego.bt

// (Otros imports...)
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable // ¡NUEVO IMPORT!
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton // ¡NUEVO IMPORT!
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.juego.GameMode // ¡NUEVO IMPORT!
import com.example.juego.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiplayerLobbyScreen(
    navController: NavController,
    viewModel: BluetoothViewModel
) {
    val state by viewModel.connectionState.collectAsState()
    val scannedDevices by viewModel.scannedDevices.collectAsState()
    val selectedGameMode by viewModel.selectedGameMode.collectAsState() // ¡NUEVO!

    // Efecto para navegar a la pantalla de juego una vez conectados
    LaunchedEffect(key1 = state) {
        if (state == ConnectionState.CONNECTED) {
            navController.navigate(Screen.Game.route) {
                // Evita que el usuario pueda "volver" al lobby
                popUpTo(Screen.Home.route)
            }
        }
    }

    // Efecto para detener el escaneo cuando el usuario sale de esta pantalla
    DisposableEffect(key1 = Unit) {
        onDispose {
            // Detenemos el escaneo y limpiamos la conexión de manera segura
            viewModel.stopDiscovery()
            viewModel.closeConnection()
            // La lógica de onCleared en el VM debería manejar esto, pero forzarlo aquí
            // puede resolver el crash en la salida del composable.
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lobby Multijugador") },
                navigationIcon = {
                    IconButton(onClick = {
                        // FIX: El popBackStack debe ser lo último para evitar un crash en la navegación
                        viewModel.stopDiscovery()
                        viewModel.closeConnection()
                        navController.popBackStack()
                    }) {
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
                .padding(16.dp)
        ) {

            // Botones de Host y Scan
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(
                    onClick = { viewModel.startServer() },
                    enabled = state == ConnectionState.IDLE
                ) {
                    Text("Ser Anfitrión")
                }
                Button(
                    onClick = { viewModel.startDiscovery() },
                    enabled = state == ConnectionState.IDLE
                ) {
                    Text("Buscar Partidas")
                }
            }

            Spacer(modifier = Modifier.height(16.dp)) // Espacio reducido

            // --- ¡NUEVO! Selector de Modo de Juego ---
            GameModeSelector(
                selectedMode = selectedGameMode,
                onModeSelected = { viewModel.selectGameMode(it) },
                // Habilitado si estamos inactivos O si ya somos el anfitrión
                isEnabled = (state == ConnectionState.IDLE || state == ConnectionState.LISTENING)
            )
            // ----------------------------------------

            Spacer(modifier = Modifier.height(24.dp))

            // Indicador de Estado
            StatusIndicator(state = state)

            Spacer(modifier = Modifier.height(24.dp))

            // Lista de Dispositivos
            DeviceList(
                devices = scannedDevices,
                onDeviceClick = { address ->
                    viewModel.connectToDevice(address)
                },
                isEnabled = state == ConnectionState.IDLE
            )
        }
    }
}

// --- ¡NUEVO COMPOSABLE! ---
@Composable
private fun GameModeSelector(
    selectedMode: GameMode,
    onModeSelected: (GameMode) -> Unit,
    isEnabled: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Seleccionar Modo de Juego",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        GameMode.entries.forEach { mode ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = (mode == selectedMode),
                        onClick = { onModeSelected(mode) },
                        enabled = isEnabled
                    )
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = (mode == selectedMode),
                    onClick = null, // onClick se maneja en el 'selectable'
                    enabled = isEnabled
                )
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = when (mode) { // Nombres más amigables
                        GameMode.CLASSIC -> "Clásico"
                        GameMode.TIME_ATTACK -> "Contrarreloj"
                        GameMode.CONFUSION -> "Confusión"
                    },
                    color = if (isEnabled) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}


@Composable
private fun StatusIndicator(state: ConnectionState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        when (state) {
            ConnectionState.LISTENING -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.width(16.dp))
                Text("Esperando a un jugador...", style = MaterialTheme.typography.titleMedium)
            }
            ConnectionState.CONNECTING -> {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.width(16.dp))
                Text("Conectando...", style = MaterialTheme.typography.titleMedium)
            }
            ConnectionState.CONNECTED -> {
                Text("¡Conectado!", style = MaterialTheme.typography.titleMedium)
            }
            else -> { // IDLE
                Text("Elige una opción", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun DeviceList(
    devices: List<BluetoothDeviceDomain>,
    onDeviceClick: (String) -> Unit,
    isEnabled: Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 8.dp)
    ) {
        item {
            Text(
                "Dispositivos Encontrados",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }
        items(devices) { device ->
            DeviceItem(
                deviceName = device.name ?: "Dispositivo Desconocido",
                deviceAddress = device.address,
                onClick = { onDeviceClick(device.address) },
                isEnabled = isEnabled
            )
        }
        if (devices.isEmpty()) {
            item {
                Text(
                    "No se encontraron dispositivos. Asegúrate de que el Anfitrión esté 'Esperando'.",
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun DeviceItem(
    deviceName: String,
    deviceAddress: String,
    onClick: () -> Unit,
    isEnabled: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = isEnabled, onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp)
    ) {
        Text(
            text = deviceName,
            style = MaterialTheme.typography.bodyLarge,
            color = if (isEnabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Text(
            text = deviceAddress,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isEnabled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
    }
}