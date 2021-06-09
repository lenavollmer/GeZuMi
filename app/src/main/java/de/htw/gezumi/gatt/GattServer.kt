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

class GattServer(private val _context: Context, private val _bluetoothController: BluetoothController, private val _connectCallback : HostFragment.GattConnectCallback) {

    var bluetoothGattServer: BluetoothGattServer? = null
    private val _bluetoothManager = _context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    val subscribedDevices = mutableSetOf<BluetoothDevice>()


    init {
        if (!_bluetoothController.isBluetoothEnabled())
            Log.d(TAG, "Bluetooth is currently disabled")
    }

    fun startServer(gameService: BluetoothGattService) {
        //_bluetoothController.startAdvertising(ParcelUuid(GameService.HOST_UUID))

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
        bluetoothGattServer?.notifyCharacteristicChanged(device, joinApprovedCharacteristic, false)
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
            bluetoothGattServer?.notifyCharacteristicChanged(device, gameStartCharacteristic, false)
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
}