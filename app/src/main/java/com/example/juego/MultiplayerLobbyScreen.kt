package com.example.juego.bt

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import com.example.juego.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiplayerLobbyScreen(
    navController: NavController,
    viewModel: BluetoothViewModel
) {
    val state by viewModel.connectionState.collectAsState()
    val scannedDevices by viewModel.scannedDevices.collectAsState()

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
            viewModel.stopDiscovery()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lobby Multijugador") },
                navigationIcon = {
                    IconButton(onClick = {
                        viewModel.closeConnection() // Cierra todo al salir
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