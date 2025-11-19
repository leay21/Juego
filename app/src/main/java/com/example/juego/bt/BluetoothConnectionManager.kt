package com.example.juego.bt

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
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
import java.io.IOException
import java.io.InputStream
import java.util.UUID

@SuppressLint("MissingPermission")
class BluetoothConnectionManager(
    private val context: Context,
    private val coroutineScope: CoroutineScope
) {
    private val serviceName = "JuegoDueloReflejos"
    private val serviceUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    private val bluetoothManager by lazy {
        context.getSystemService(BluetoothManager::class.java)
    }
    private val bluetoothAdapter: BluetoothAdapter? by lazy {
        bluetoothManager?.adapter
    }

    private val gson = Gson()

    private val _connectionState = MutableStateFlow(ConnectionState.IDLE)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()

    private val _scannedDevices = MutableStateFlow<List<BluetoothDeviceDomain>>(emptyList())
    val scannedDevices: StateFlow<List<BluetoothDeviceDomain>> = _scannedDevices.asStateFlow()

    private val _receivedMessages = MutableSharedFlow<BluetoothMessage>()
    val receivedMessages: SharedFlow<BluetoothMessage> = _receivedMessages.asSharedFlow()

    private var currentSocket: BluetoothSocket? = null
    private var serverSocket: BluetoothServerSocket? = null
    private var dataTransferJob: Job? = null

    private val bluetoothStateReceiver = BluetoothStateReceiver { device ->
        _scannedDevices.update {
            if (device !in it) it + device else it
        }
    }

    val hasBluetoothAdapter: Boolean
        get() = bluetoothAdapter != null

    private fun hasPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    // --- Lógica de Escaneo ---
    fun startDiscovery() {
        val adapter = bluetoothAdapter ?: return
        if (!adapter.isEnabled) return

        val permissionToCheck = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            Manifest.permission.BLUETOOTH_SCAN
        else
            Manifest.permission.ACCESS_FINE_LOCATION

        if (!hasPermission(permissionToCheck)) {
            Log.e("BTManager", "Falta el permiso $permissionToCheck para iniciar el descubrimiento.")
            return
        }

        try {
            context.registerReceiver(
                bluetoothStateReceiver,
                IntentFilter(BluetoothDevice.ACTION_FOUND)
            )
            Log.d("BTManager", "Receiver registrado.")
        } catch (e: IllegalArgumentException) {
            Log.w("BTManager", "Receiver ya estaba registrado, continuando el escaneo.", e)
        } catch (e: SecurityException) {
            Log.e("BTManager", "SecurityException al registrar receiver.", e)
            _connectionState.value = ConnectionState.IDLE
            return
        } catch (e: Exception) {
            Log.e("BTManager", "Error inesperado al registrar el receiver.", e)
            _connectionState.value = ConnectionState.IDLE
            return
        }

        try {
            _scannedDevices.value = emptyList()
            adapter.startDiscovery()
            Log.d("BTManager", "Iniciando escaneo...")
        } catch (e: SecurityException) {
            Log.e("BTManager", "SecurityException al iniciar escaneo.", e)
            stopDiscoveryCleanup()
            _connectionState.value = ConnectionState.IDLE
        }
    }

    private fun stopDiscoveryCleanup() {
        try {
            context.unregisterReceiver(bluetoothStateReceiver)
            Log.d("BTManager", "Receiver desregistrado exitosamente.")
        } catch (e: IllegalArgumentException) {
            Log.w("BTManager", "Receiver no estaba registrado al intentar desregistrar.", e)
        } catch (e: Exception) {
            Log.e("BTManager", "Error al intentar desregistrar el receiver.", e)
        }
    }

    fun stopDiscovery() {
        val adapter = bluetoothAdapter ?: return
        if (!adapter.isEnabled) return

        val permissionToCheck = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            Manifest.permission.BLUETOOTH_SCAN
        else
            Manifest.permission.ACCESS_FINE_LOCATION

        if (!hasPermission(permissionToCheck)) {
            Log.e("BTManager", "Falta el permiso $permissionToCheck para detener el descubrimiento.")
            return
        }

        try {
            adapter.cancelDiscovery()
            Log.d("BTManager", "Descubrimiento cancelado.")
        } catch (e: SecurityException) {
            Log.w("BTManager", "SecurityException al cancelar descubrimiento.", e)
        }

        stopDiscoveryCleanup()
    }

    // --- Lógica de Anfitrión (Host) ---
    fun startServer() {
        if (_connectionState.value != ConnectionState.IDLE) return

        val adapter = bluetoothAdapter ?: run {
            Log.e("BTManager", "Adaptador Bluetooth es nulo.")
            return
        }

        val permissionToCheck = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            Manifest.permission.BLUETOOTH_CONNECT
        else
            Manifest.permission.BLUETOOTH

        if (!hasPermission(permissionToCheck)) {
            Log.e("BTManager", "Falta el permiso $permissionToCheck para iniciar el servidor.")
            return
        }

        _connectionState.value = ConnectionState.LISTENING
        coroutineScope.launch(Dispatchers.IO) {
            try {
                delay(200)
                serverSocket = adapter.listenUsingRfcommWithServiceRecord(serviceName, serviceUUID)
                Log.d("BTManager", "Servidor iniciado, esperando conexión...")

                val clientSocket = serverSocket?.accept()

                if (clientSocket != null) {
                    Log.d("BTManager", "Cliente aceptado. Cerrando socket de servidor.")
                    serverSocket?.close()
                    serverSocket = null
                    onConnectionEstablished(clientSocket)
                } else {
                    if (_connectionState.value == ConnectionState.LISTENING) {
                        throw IOException("Socket de servidor nulo o cerrado.")
                    }
                }
            } catch (e: IOException) {
                if (_connectionState.value == ConnectionState.LISTENING) {
                    Log.e("BTManager", "Error al iniciar servidor o aceptar conexión", e)
                    closeConnection()
                }
            } catch (e: SecurityException) {
                Log.e("BTManager", "Error de seguridad al iniciar servidor.", e)
                closeConnection()
            }
        }
    }

    // --- Lógica de Cliente (Client) ---
    fun connectToDevice(deviceAddress: String) {
        if (_connectionState.value != ConnectionState.IDLE) return

        val adapter = bluetoothAdapter ?: run {
            Log.e("BTManager", "Adaptador Bluetooth es nulo.")
            return
        }

        val permissionToCheck = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            Manifest.permission.BLUETOOTH_CONNECT
        else
            Manifest.permission.BLUETOOTH

        if (!hasPermission(permissionToCheck)) {
            Log.e("BTManager", "Falta el permiso $permissionToCheck para conectar.")
            return
        }

        val device = adapter.getRemoteDevice(deviceAddress) ?: run {
            Log.e("BTManager", "Dispositivo no encontrado: $deviceAddress")
            return
        }

        _connectionState.value = ConnectionState.CONNECTING
        coroutineScope.launch(Dispatchers.IO) {
            try {
                if(adapter.isDiscovering) {
                    stopDiscovery()
                }
                delay(200)
                val socket = device.createRfcommSocketToServiceRecord(serviceUUID)
                socket.connect()

                Log.d("BTManager", "Conectado al servidor.")
                onConnectionEstablished(socket)
            } catch (e: IOException) {
                Log.e("BTManager", "Error al conectar con el servidor", e)
                closeConnection()
            } catch (e: SecurityException) {
                Log.e("BTManager", "Error de seguridad al conectar.", e)
                closeConnection()
            }
        }
    }

    // [CORRECCIÓN 1]: Iniciar la escucha inmediatamente al conectar
    private fun onConnectionEstablished(socket: BluetoothSocket) {
        currentSocket = socket
        _connectionState.value = ConnectionState.CONNECTED

        // Iniciamos la corrutina de escucha y guardamos el Job
        dataTransferJob = coroutineScope.launch(Dispatchers.IO) {
            listenForData(socket.inputStream)
        }
    }

    // [CORRECCIÓN 2]: Loop de lectura robusto con Gson
    private suspend fun listenForData(inputStream: InputStream) {
        try {
            val reader = inputStream.bufferedReader()
            while (true) {
                // readLine() espera hasta encontrar un \n enviado por el otro lado
                val line = reader.readLine() ?: break // Si es null, el socket se cerró

                try {
                    // Convertimos el JSON recibido al objeto BluetoothMessage
                    val message = gson.fromJson(line, BluetoothMessage::class.java)
                    _receivedMessages.emit(message)
                } catch (e: Exception) {
                    Log.e("BTManager", "Error procesando mensaje JSON: $line", e)
                }
            }
        } catch (e: IOException) {
            Log.e("BTManager", "Error en la conexión de datos (lectura)", e)
        } finally {
            // Si el loop termina, aseguramos el cierre
            Log.d("BTManager", "Fin de escucha de datos.")
            closeConnection()
        }
    }

    // [CORRECCIÓN 3]: Envío con salto de línea (\n) y flush
    fun sendMessage(message: BluetoothMessage) {
        val socket = currentSocket ?: run {
            Log.e("BTManager", "No hay socket conectado para enviar mensaje.")
            return
        }

        coroutineScope.launch(Dispatchers.IO) {
            try {
                val json = gson.toJson(message)
                // IMPORTANTE: Agregar \n para que readLine() del receptor sepa que terminó el mensaje
                val jsonWithNewline = "$json\n"

                socket.outputStream.write(jsonWithNewline.toByteArray())
                socket.outputStream.flush() // Forzar el envío inmediato
            } catch (e: IOException) {
                Log.e("BTManager", "Error al enviar datos", e)
                closeConnection()
            }
        }
    }

    fun closeConnection() {
        Log.d("BTManager", "Cerrando conexión...")
        try {
            dataTransferJob?.cancel()
            stopDiscoveryCleanup()
            currentSocket?.close()
            serverSocket?.close()
            currentSocket = null
            serverSocket = null
            _connectionState.value = ConnectionState.IDLE
        } catch (e: Exception) {
            Log.e("BTManager", "Error al cerrar sockets.", e)
            _connectionState.value = ConnectionState.IDLE
        }
    }
}