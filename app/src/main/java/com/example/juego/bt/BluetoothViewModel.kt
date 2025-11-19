package com.example.juego.bt

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.juego.GameMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import android.util.Log // ¡IMPORT CORREGIDO!

class BluetoothViewModel(
    private val connectionManager: BluetoothConnectionManager
) : ViewModel() {

    val connectionState: StateFlow<ConnectionState> = connectionManager.connectionState
    val scannedDevices: StateFlow<List<BluetoothDeviceDomain>> = connectionManager.scannedDevices
    val receivedMessages = connectionManager.receivedMessages

    private val _selectedGameMode = MutableStateFlow(GameMode.CLASSIC)
    val selectedGameMode: StateFlow<GameMode> = _selectedGameMode.asStateFlow()
    // --- [NUEVO] Flag para recordar el rol (Host vs Cliente) ---
    private val _isHost = MutableStateFlow(false)
    val isHost: StateFlow<Boolean> = _isHost.asStateFlow()
    // -----------------------------------------------------------

    fun selectGameMode(mode: GameMode) {
        _selectedGameMode.value = mode
    }

    val hasBluetoothAdapter: Boolean
        get() = connectionManager.hasBluetoothAdapter

    fun startDiscovery() {
        connectionManager.startDiscovery()
    }

    fun stopDiscovery() {
        connectionManager.stopDiscovery()
    }

    fun startServer() {
        _isHost.value = true // [MODIFICADO] Marcamos que somos Host
        connectionManager.startServer()
    }

    fun connectToDevice(deviceAddress: String) {
        _isHost.value = false // [MODIFICADO] Marcamos que somos Cliente
        connectionManager.connectToDevice(deviceAddress)
    }

    fun closeConnection() {
        connectionManager.closeConnection()
    }

    fun sendMessage(message: BluetoothMessage) {
        viewModelScope.launch {
            connectionManager.sendMessage(message)
        }
    }

    /**
     * ¡ULTIMA MODIFICACIÓN! Asegurar que la limpieza es fail-safe.
     */
    override fun onCleared() {
        super.onCleared()
        // Envolvemos todo en un try-catch para evitar que el VM crashee la app
        // si el SO falla al liberar el socket o el receiver.
        try {
            stopDiscovery()
            closeConnection()
        } catch (e: Exception) {
            // Ignoramos la excepción en la limpieza final, ya que el recurso iba a morir de todas formas.
            Log.e("BluetoothViewModel", "Error seguro al limpiar Bluetooth: ${e.message}")
        }
    }
}