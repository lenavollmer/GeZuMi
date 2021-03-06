package de.htw.gezumi.gatt

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager
import android.content.Context
import android.util.Log
import de.htw.gezumi.HostFragment
import de.htw.gezumi.Utils
import de.htw.gezumi.controller.BluetoothController
import de.htw.gezumi.model.BluetoothData
import java.nio.ByteBuffer

private const val TAG = "GattServer"

class GattServer(_context: Context, private val _bluetoothController: BluetoothController, private val _connectCallback : HostFragment.GattConnectCallback) {

    var bluetoothGattServer: BluetoothGattServer? = null
    private val _bluetoothManager = _context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    val subscribedDevices = mutableSetOf<BluetoothDevice>()

    fun startServer(gameService: BluetoothGattService) {
        Log.d(TAG, "start gatt server")
        bluetoothGattServer = _bluetoothController.openGattServer(GattServerCallback( this, _connectCallback))

        bluetoothGattServer?.addService(gameService)
            ?: Log.d(TAG, "Unable to create GATT server")
    }

    /**
     * Shut down the GATT server.
     */
    fun stopServer() {
        if (_bluetoothManager.adapter.isEnabled) {
            Log.d(TAG, "stop gatt server")
            subscribedDevices.forEach{ bluetoothGattServer?.cancelConnection(it) }
            subscribedDevices.clear()
            bluetoothGattServer?.close()
        }
    }

    fun notifyJoinApproved(device: BluetoothDevice, approved: Boolean) {
        val joinApprovedCharacteristic = bluetoothGattServer?.getService(GameService.HOST_UUID)?.getCharacteristic(GameService.JOIN_APPROVED_UUID)
        joinApprovedCharacteristic?.value = ByteBuffer.allocate(4).putInt(if (approved) 1 else 0).array()
        Log.d(TAG, "notify join approve: $approved")
        bluetoothGattServer?.notifyCharacteristicChanged(device, joinApprovedCharacteristic, true)
    }

    fun notifyGameStart() {
        Log.d(TAG, "notify game start")
        if (subscribedDevices.isEmpty()) {
            Log.i(TAG, "No subscribers registered")
            return
        }

        val gameStartCharacteristic = bluetoothGattServer?.getService(GameService.HOST_UUID)?.getCharacteristic(GameService.GAME_EVENT_UUID)
        gameStartCharacteristic?.value = ByteBuffer.allocate(4).putInt(GameService.GAME_START_EVENT).array()
        for (device in subscribedDevices) {
            bluetoothGattServer?.notifyCharacteristicChanged(device, gameStartCharacteristic, true)
        }
    }

    fun notifyGameEnding(){
        Log.d(TAG, "notify game ending")
        if (subscribedDevices.isEmpty()) {
            Log.i(TAG, "No subscribers registered")
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
        Log.d(TAG, "notify host update sender: ${Utils.toHexString(bluetoothData.senderId)} distance to: ${Utils.toHexString(bluetoothData.id)}")
        val hostUpdateCharacteristic = bluetoothGattServer?.getService(GameService.HOST_UUID)?.getCharacteristic(GameService.HOST_UPDATE_UUID)
        hostUpdateCharacteristic?.value = bluetoothData.toByteArray()
        for (device in subscribedDevices) {
            bluetoothGattServer?.notifyCharacteristicChanged(device, hostUpdateCharacteristic, false)
        }
    }

    @kotlin.ExperimentalUnsignedTypes
    fun indicateHostUpdate(bluetoothData: BluetoothData) {
        Log.d(TAG, "indicate host update sender: ${Utils.toHexString(bluetoothData.senderId)}")
        val responseHostUpdateCharacteristic = bluetoothGattServer?.getService(GameService.HOST_UUID)?.getCharacteristic(GameService.RESPONSE_HOST_UPDATE_UUID)
        responseHostUpdateCharacteristic?.value = bluetoothData.toByteArray()
        for (device in subscribedDevices) {
            bluetoothGattServer?.notifyCharacteristicChanged(device, responseHostUpdateCharacteristic, true)
        }
    }
}