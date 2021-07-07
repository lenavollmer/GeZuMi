package de.htw.gezumi.gatt

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattDescriptor
import android.content.Context
import de.htw.gezumi.model.BluetoothData

class GattClient(private val _context: Context) {

    private var _gatt: BluetoothGatt? = null

    fun connect(hostDevice: BluetoothDevice, gattClientCallback: GattClientCallback) {
        _gatt = hostDevice.connectGatt(_context, false, gattClientCallback)
    }

    fun disconnect() {
        val subscribeDescriptor = _gatt?.getService(GameService.HOST_UUID)?.getCharacteristic(GameService.GAME_EVENT_UUID)?.getDescriptor(GameService.CLIENT_CONFIG)
        subscribeDescriptor?.value = BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
        _gatt?.writeDescriptor(subscribeDescriptor)
        _gatt?.setCharacteristicNotification(
                _gatt?.getService(GameService.HOST_UUID)
                    ?.getCharacteristic(GameService.JOIN_APPROVED_UUID), false
        )
        _gatt?.setCharacteristicNotification(_gatt?.getService(GameService.HOST_UUID)?.getCharacteristic(GameService.GAME_EVENT_UUID), false)
        _gatt?.setCharacteristicNotification(_gatt?.getService(GameService.HOST_UUID)?.getCharacteristic(GameService.HOST_UPDATE_UUID), false)
        _gatt?.disconnect()
    }

    @kotlin.ExperimentalUnsignedTypes
    @SuppressLint("DefaultLocale")
    fun sendPlayerUpdate(bluetoothData: BluetoothData) {
        val playerUpdateCharacteristic =
            _gatt?.getService(GameService.HOST_UUID)?.getCharacteristic(GameService.PLAYER_UPDATE_UUID)
        playerUpdateCharacteristic?.value = bluetoothData.toByteArray()
        if (playerUpdateCharacteristic != null) {
            _gatt?.writeCharacteristic(playerUpdateCharacteristic)
        }
    }
}