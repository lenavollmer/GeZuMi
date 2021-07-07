package de.htw.gezumi.gatt

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.content.Context
import de.htw.gezumi.HostFragment
import de.htw.gezumi.controller.BluetoothController
import de.htw.gezumi.model.BluetoothData
import java.nio.ByteBuffer

class GattServer(_context: Context, private val _bluetoothController: BluetoothController, private val _connectCallback : HostFragment.GattConnectCallback) {

    var bluetoothGattServer: BluetoothGattServer? = null
    private val _bluetoothManager = _context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    val subscribedDevices = mutableSetOf<BluetoothDevice>()

    fun startServer(gameService: BluetoothGattService) {
        bluetoothGattServer = _bluetoothController.openGattServer(GattServerCallback( this, _connectCallback))
        bluetoothGattServer?.addService(gameService)
    }

    /**
     * Shut down the GATT server.
     */
    fun stopServer() {
        if (_bluetoothManager.adapter.isEnabled) {
            subscribedDevices.forEach{ bluetoothGattServer?.cancelConnection(it) }
            subscribedDevices.clear()
            bluetoothGattServer?.close()
        }
    }

    fun notifyJoinApproved(device: BluetoothDevice, approved: Boolean) {
        val joinApprovedCharacteristic = bluetoothGattServer?.getService(GameService.HOST_UUID)?.getCharacteristic(GameService.JOIN_APPROVED_UUID)
        joinApprovedCharacteristic?.value = ByteBuffer.allocate(4).putInt(if (approved) 1 else 0).array()
        bluetoothGattServer?.notifyCharacteristicChanged(device, joinApprovedCharacteristic, true)
    }

    fun notifyGameStart() {
        if (subscribedDevices.isEmpty()) {
            return
        }

        val gameStartCharacteristic = bluetoothGattServer?.getService(GameService.HOST_UUID)?.getCharacteristic(GameService.GAME_EVENT_UUID)
        gameStartCharacteristic?.value = ByteBuffer.allocate(4).putInt(GameService.GAME_START_EVENT).array()
        for (device in subscribedDevices) {
            bluetoothGattServer?.notifyCharacteristicChanged(device, gameStartCharacteristic, true)
        }
    }

    fun notifyGameEnding(){
        if (subscribedDevices.isEmpty()) {
            return
        }

        val gameEndCharacteristic = bluetoothGattServer?.getService(GameService.HOST_UUID)?.getCharacteristic(GameService.GAME_EVENT_UUID)
        gameEndCharacteristic?.value = ByteBuffer.allocate(4).putInt(GameService.GAME_END_EVENT).array()
        for (device in subscribedDevices) {
            bluetoothGattServer?.notifyCharacteristicChanged(device, gameEndCharacteristic, true)
        }
    }

    @kotlin.ExperimentalUnsignedTypes
    @SuppressLint("DefaultLocale")
    fun notifyHostUpdate(bluetoothData: BluetoothData) {
        val hostUpdateCharacteristic = bluetoothGattServer?.getService(GameService.HOST_UUID)?.getCharacteristic(GameService.HOST_UPDATE_UUID)
        hostUpdateCharacteristic?.value = bluetoothData.toByteArray()
        for (device in subscribedDevices) {
            bluetoothGattServer?.notifyCharacteristicChanged(device, hostUpdateCharacteristic, false)
        }
    }

    @kotlin.ExperimentalUnsignedTypes
    fun indicateHostUpdate(bluetoothData: BluetoothData) {
        val responseHostUpdateCharacteristic = bluetoothGattServer?.getService(GameService.HOST_UUID)?.getCharacteristic(GameService.RESPONSE_HOST_UPDATE_UUID)
        responseHostUpdateCharacteristic?.value = bluetoothData.toByteArray()
        for (device in subscribedDevices) {
            bluetoothGattServer?.notifyCharacteristicChanged(device, responseHostUpdateCharacteristic, true)
        }
    }
}