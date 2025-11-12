package com.example.juego.bt

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BluetoothStateReceiver(
    private val onDeviceFound: (BluetoothDeviceDomain) -> Unit
) : BroadcastReceiver() {

    @SuppressLint("MissingPermission") // Los permisos se piden en MainActivity
    override fun onReceive(context: Context?, intent: Intent?) {
        when (intent?.action) {
            // Un dispositivo fue encontrado
            BluetoothDevice.ACTION_FOUND -> {
                val device: BluetoothDevice? =
                    intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)

                device?.let {
                    // Lo convertimos a nuestro modelo de dominio y lo pasamos
                    onDeviceFound(
                        BluetoothDeviceDomain(
                            name = it.name,
                            address = it.address
                        )
                    )
                }
            }
        }
    }
}