package com.example.juego.bt

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import android.util.Log
import com.example.juego.GameUiState
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

@SuppressLint("MissingPermission") // Los permisos se piden en MainActivity
class BluetoothConnectionManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    // Nombre único para el servicio de nuestra app
    private val serviceName = "JuegoDueloReflejos"
    // UUID único para nuestra app. Debe ser el mismo en ambos dispositivos.
    private val serviceUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // UUID estándar para SPP

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager?.adapter
    }

    private val gson = Gson()

    // --- Estado de la Conexión ---
    private val _connectionState = MutableStateFlow(ConnectionState.IDLE)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    // --- Dispositivos Escaneados ---
    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    val scannedDevices: StateFlow<List<BluetoothDeviceDomain>> = _scannedDevices.asStateFlow()

    // --- Mensajes Recibidos ---
    private val _receivedMessages = MutableSharedFlow<BluetoothMessage>()
    val receivedMessages: SharedFlow<BluetoothMessage> = _receivedMessages.asSharedFlow()

    // --- Sockets y Jobs ---
    private var currentSocket: BluetoothSocket? = null
    private var serverSocket: BluetoothServerSocket? = null
    private var dataTransferJob: Job? = null

    private val bluetoothStateReceiver = BluetoothStateReceiver { device ->
        _scannedDevices.update {
            if (device !in it) it + device else it
        }
    }

    // --- ¡NUEVA! Propiedad de ayuda ---
    val hasBluetoothAdapter: Boolean
        get() = bluetoothAdapter != null

    // --- Lógica de Escaneo ---
    fun startDiscovery() {
        // ¡CORREGIDO! Asignar a una variable local
        val adapter = bluetoothAdapter ?: return
        if (!adapter.isEnabled) return

        // Registrar el BroadcastReceiver
        context.registerReceiver(
            bluetoothStateReceiver,
            IntentFilter(BluetoothDevice.ACTION_FOUND)
        )

        _scannedDevices.value = emptyList()
        adapter.startDiscovery() // Usar la variable local
        Log.d("BTManager", "Iniciando escaneo...")
    }

    fun stopDiscovery() {
        // ¡CORREGIDO! Asignar a una variable local
        val adapter = bluetoothAdapter ?: return
        if (!adapter.isEnabled) return

        try {
            adapter.cancelDiscovery() // Usar la variable local
            context.unregisterReceiver(bluetoothStateReceiver)
            Log.d("BTManager", "Escaneo detenido.")
        } catch (e: IllegalArgumentException) {
            Log.e("BTManager", "Receiver de escaneo no estaba registrado.", e)
        }
    }

    // --- Lógica de Anfitrión (Host) ---
    fun startServer() {
        if (_connectionState.value != ConnectionState.IDLE) return

        // ¡CORREGIDO! Asignar a una variable local
        val adapter = bluetoothAdapter ?: run {
            Log.e("BTManager", "Adaptador Bluetooth es nulo.")
            return
        }

        _connectionState.value = ConnectionState.LISTENING
        coroutineScope.launch(Dispatchers.IO) {
            try {
                serverSocket = adapter.listenUsingRfcommWithServiceRecord(serviceName, serviceUUID) // Usar la variable local

                Log.d("BTManager", "Servidor iniciado, esperando conexión...")
                val clientSocket = serverSocket?.accept()

                if (clientSocket != null) {
                    Log.d("BTManager", "Cliente aceptado. Cerrando socket de servidor.")
                    serverSocket?.close()
                    serverSocket = null
                    onConnectionEstablished(clientSocket)
                } else {
                    // Si el serverSocket es nulo (ej. se cerró la conexión), no lanzar excepción
                    if (_connectionState.value == ConnectionState.LISTENING) {
                        throw IOException("Socket de servidor nulo o cerrado.")
                    }
                }
            } catch (e: IOException) {
                if (_connectionState.value == ConnectionState.LISTENING) {
                    Log.e("BTManager", "Error al iniciar servidor o aceptar conexión", e)
                    closeConnection()
                }
            }
        }
    }

    // --- Lógica de Cliente (Client) ---
    fun connectToDevice(deviceAddress: String) {
        if (_connectionState.value != ConnectionState.IDLE) return

        // ¡CORREGIDO! Asignar a una variable local
        val adapter = bluetoothAdapter ?: run {
            Log.e("BTManager", "Adaptador Bluetooth es nulo.")
            return
        }

        val device = adapter.getRemoteDevice(deviceAddress) // Usar la variable local
        if (device == null) {
            Log.e("BTManager", "Dispositivo no encontrado con la dirección $deviceAddress")
            return
        }

        _connectionState.value = ConnectionState.CONNECTING
        coroutineScope.launch(Dispatchers.IO) {
            try {
                // Asegurarse de detener el escaneo ANTES de conectar
                if(adapter.isDiscovering) {
                    stopDiscovery()
                }

                val socket = device.createRfcommSocketToServiceRecord(serviceUUID)
                socket.connect() // Llamada bloqueante

                Log.d("BTManager", "Conectado al servidor.")
                onConnectionEstablished(socket)
            } catch (e: IOException) {
                Log.e("BTManager", "Error al conectar con el servidor", e)
                closeConnection()
            }
        }
    }

    // --- Lógica Común (Conexión Establecida) ---
    private fun onConnectionEstablished(socket: BluetoothSocket) {
        currentSocket = socket
        _connectionState.value = ConnectionState.CONNECTED

        dataTransferJob = coroutineScope.launch(Dispatchers.IO) {
            try {
                listenForData(socket.inputStream) // Bucle infinito para leer
            } catch (e: IOException) {
                Log.e("BTManager", "Conexión perdida.", e)
                closeConnection()
            }
        }
    }

    // --- Escuchar Datos ---
    private suspend fun listenForData(inputStream: InputStream) {
        val reader = inputStream.bufferedReader()
        // Bucle infinito que se rompe por la excepción si la conexión se cierra
        while (true) {
            val line = reader.readLine()
            if (line == null) {
                Log.d("BTManager", "Stream cerrado, terminando escucha.")
                break // El stream se cerró, sal del bucle
            }

            Log.d("BTManager", "Datos recibidos: $line")
            try {
                // Deserializa el JSON de vuelta a un objeto BluetoothMessage
                val message = gson.fromJson(line, BluetoothMessage::class.java)
                withContext(Dispatchers.Main) {
                    _receivedMessages.emit(message)
                }
            } catch (e: Exception) {
                Log.e("BTManager", "Error al parsear JSON: $line", e)
            }
        }
    }

    // --- Enviar Datos ---
    fun sendMessage(message: BluetoothMessage) {
        val outputStream = currentSocket?.outputStream ?: run {
            Log.e("BTManager", "No hay socket conectado para enviar mensaje.")
            return
        }

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val jsonMessage = gson.toJson(message)

                outputStream.write((jsonMessage + "\n").toByteArray())
                outputStream.flush()
                Log.d("BTManager", "Datos enviados: $jsonMessage")
            } catch (e: IOException) {
                Log.e("BTManager", "Error al enviar datos", e)
                closeConnection()
            }
        }
    }

    // --- Cerrar Conexión ---
    fun closeConnection() {
        Log.d("BTManager", "Cerrando conexión...")
        try {
            dataTransferJob?.cancel()
            currentSocket?.close()
            serverSocket?.close()
            currentSocket = null
            serverSocket = null
            _connectionState.value = ConnectionState.IDLE
        } catch (e: IOException) {
            Log.e("BTManager", "Error al cerrar sockets", e)
        }
    }
}