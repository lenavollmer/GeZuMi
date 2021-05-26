package de.htw.gezumi.gatt

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.content.Context
import android.util.Log
import de.htw.gezumi.model.DeviceData
import java.nio.ByteBuffer
import java.util.*

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

    fun sendPlayerUpdate(deviceData: DeviceData) {
        val playerUpdateCharacteristic = _gatt.getService(GameService.HOST_UUID)?.getCharacteristic(GameService.PLAYER_UPDATE_UUID)
        playerUpdateCharacteristic?.value = deviceData.toByteArray()
        Log.d(TAG, "send player update: ${deviceData.deviceAddress}, values=${deviceData.values.contentToString()}")
        _gatt.writeCharacteristic(playerUpdateCharacteristic)
    }
}