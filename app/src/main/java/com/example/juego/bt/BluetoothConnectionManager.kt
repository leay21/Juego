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
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import android.Manifest
import android.os.Build
import kotlinx.coroutines.delay

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

    // Se elimina la bandera isReceiverRegistered

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

        // FIX CRÍTICO: Intentar registrar el Receiver dentro de un try-catch que maneje el estado
        try {
            context.registerReceiver(
                bluetoothStateReceiver,
                IntentFilter(BluetoothDevice.ACTION_FOUND)
            )
            Log.d("BTManager", "Receiver registrado.")
        } catch (e: IllegalArgumentException) {
            // El Receiver ya estaba registrado. Esto es aceptable, simplemente lo ignoramos.
            Log.w("BTManager", "Receiver ya estaba registrado, continuando el escaneo.", e)
        } catch (e: SecurityException) {
            Log.e("BTManager", "SecurityException al registrar receiver. No se puede escanear.", e)
            _connectionState.value = ConnectionState.IDLE
            return
        } catch (e: Exception) {
            Log.e("BTManager", "Error inesperado al registrar el receiver.", e)
            _connectionState.value = ConnectionState.IDLE
            return
        }

        // El escaneo puede lanzar SecurityException, así que también lo cubrimos
        try {
            _scannedDevices.value = emptyList()
            adapter.startDiscovery()
            Log.d("BTManager", "Iniciando escaneo...")
        } catch (e: SecurityException) {
            Log.e("BTManager", "SecurityException al iniciar escaneo. No se puede escanear.", e)
            // Si falla aquí, debemos intentar desregistrar el receiver inmediatamente
            stopDiscoveryCleanup()
            _connectionState.value = ConnectionState.IDLE
        }
    }

    /**
     * Función separada para la limpieza y desregistro.
     * Es llamada por stopDiscovery y también si startDiscovery falla.
     */
    private fun stopDiscoveryCleanup() {
        // Intentar desregistrar el receiver, capturando la excepción si no está registrado
        try {
            context.unregisterReceiver(bluetoothStateReceiver)
            Log.d("BTManager", "Receiver desregistrado exitosamente.")
        } catch (e: IllegalArgumentException) {
            // Esto es normal si no estaba registrado. Ignoramos para prevenir el crash al salir.
            Log.w("BTManager", "Receiver no estaba registrado al intentar desregistrar. Ignorando.", e)
        } catch (e: Exception) {
            // Captura cualquier otro error de limpieza
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
            Log.w("BTManager", "SecurityException al cancelar descubrimiento. Ignorando.", e)
        }

        stopDiscoveryCleanup() // Llamamos a la función de limpieza
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
                Log.e("BTManager", "Error de seguridad (permisos) al iniciar servidor. Intente reiniciar la app.", e)
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

        val device = adapter.getRemoteDevice(deviceAddress)
        if (device == null) {
            Log.e("BTManager", "Dispositivo no encontrado con la dirección $deviceAddress")
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
                Log.e("BTManager", "Error de seguridad (permisos) al conectar con el servidor", e)
                closeConnection()
            }
        }
    }

    private fun onConnectionEstablished(socket: BluetoothSocket) {
        currentSocket = socket
        _connectionState.value = ConnectionState.CONNECTED
        // ... (resto del código) ...
    }

    private suspend fun listenForData(inputStream: InputStream) {
        val reader = inputStream.bufferedReader()
        while (true) {
            val line = reader.readLine()
            if (line == null) {
                Log.d("BTManager", "Stream cerrado, terminando escucha.")
                break
            }
            // ... (resto del código) ...
        }
    }

    fun sendMessage(message: com.example.juego.bt.BluetoothMessage) {
        val outputStream = currentSocket?.outputStream ?: run {
            Log.e("BTManager", "No hay socket conectado para enviar mensaje.")
            return
        }

        coroutineScope.launch(Dispatchers.IO) {
            try {
                // ... (resto del código) ...
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
            stopDiscoveryCleanup() // Nos aseguramos de limpiar el receiver aquí también
            currentSocket?.close()
            serverSocket?.close()
            currentSocket = null
            serverSocket = null
            _connectionState.value = ConnectionState.IDLE
        } catch (e: Exception) {
            Log.e("BTManager", "Error crítico al cerrar sockets de forma segura. Ignorando para prevenir crash.", e)
            _connectionState.value = ConnectionState.IDLE
        }
    }
}