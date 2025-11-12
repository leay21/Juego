package com.example.juego.bt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.juego.GameMode // ¡NUEVO IMPORT!
import kotlinx.coroutines.flow.MutableStateFlow // ¡NUEVO IMPORT!
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow // ¡NUEVO IMPORT!
import kotlinx.coroutines.launch

class BluetoothViewModel(
    private val connectionManager: BluetoothConnectionManager
) : ViewModel() {

    // Exponer los estados del manager a la UI
    val connectionState: StateFlow<ConnectionState> = connectionManager.connectionState
    val scannedDevices: StateFlow<List<BluetoothDeviceDomain>> = connectionManager.scannedDevices
    val receivedMessages = connectionManager.receivedMessages

    // --- ¡NUEVO! Estado para el modo de juego seleccionado ---
    private val _selectedGameMode = MutableStateFlow(GameMode.CLASSIC)
    val selectedGameMode: StateFlow<GameMode> = _selectedGameMode.asStateFlow()

    fun selectGameMode(mode: GameMode) {
        _selectedGameMode.value = mode
    }
    // --------------------------------------------------------

    // Propiedad de ayuda para la UI
    val hasBluetoothAdapter: Boolean
        get() = connectionManager.hasBluetoothAdapter

    // --- Funciones que la UI puede llamar ---

    fun startDiscovery() {
        connectionManager.startDiscovery()
    }

    fun stopDiscovery() {
        connectionManager.stopDiscovery()
    }

    fun startServer() {
        connectionManager.startServer()
    }

    fun connectToDevice(deviceAddress: String) {
        connectionManager.connectToDevice(deviceAddress)
    }

    fun closeConnection() {
        connectionManager.closeConnection()
    }

    /**
     * Envía un mensaje Bluetooth usando el scope del ViewModel.
     */
    fun sendMessage(message: BluetoothMessage) {
        viewModelScope.launch {
            connectionManager.sendMessage(message)
        }
    }

    /**
     * Limpia la conexión cuando el ViewModel se destruye
     * (ej. el usuario sale de la pantalla de multijugador).
     */
    override fun onCleared() {
        super.onCleared()
        // Detiene el escaneo y cierra cualquier conexión activa
        stopDiscovery()
        closeConnection()
    }
}