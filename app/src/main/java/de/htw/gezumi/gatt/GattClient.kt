package de.htw.gezumi.gatt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.content.Context
import android.util.Log

private const val TAG = "GattClient"

class GattClient(private val _context: Context) {

    private var _gatt: BluetoothGatt? = null

    fun connect(hostDevice: BluetoothDevice, gattClientCallback: GattClientCallback) {
        _gatt = hostDevice.connectGatt(_context, false, gattClientCallback)
        reconnect()
    }

    fun reconnect() {
        val success = _gatt?.connect()
        Log.d(TAG, "connected to gatt: $success")
    }

    fun disconnect() {
        _gatt?.disconnect()
    }
}