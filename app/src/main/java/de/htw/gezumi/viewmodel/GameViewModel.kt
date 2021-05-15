package de.htw.gezumi.viewmodel

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import de.htw.gezumi.controller.BluetoothController
import de.htw.gezumi.model.Device

private const val TAG = "GameViewModel"
const val RSSI_READ_INTERVAL = 500

class GameViewModel(application: Application) : AndroidViewModel(application) {

    val bluetoothController: BluetoothController = BluetoothController()
    private val _devices = mutableMapOf<Device, Long>()
    val devices: Set<Device> get() = _devices.keys

    lateinit var host: Device // is null for host themselves
    lateinit var gameId: String

    interface GameJoinCallback {
        fun onGameJoin()
        fun onGameLeave()
    }
    val gameJoinCallback = object : GameJoinCallback {
        override fun onGameJoin() {
            Log.d(TAG, "on game join")
            Handler(Looper.getMainLooper()).post{ Toast.makeText(application.applicationContext, "Joined", Toast.LENGTH_LONG).show()}
            val gameUuid = ParcelUuid.fromString(gameId)
            // waiting for game start is not necessary
            Log.d(TAG, "start advertising on game id")
            bluetoothController.startAdvertising(gameUuid)
            Log.d(TAG, "start scanning for players on game id")
            bluetoothController.scanForDevices(gameScanCallback, gameUuid)
        }

        override fun onGameLeave() {
            Log.d(TAG, "on game leave")
            // TODO
            Handler(Looper.getMainLooper()).post{}
        }
    }

    val gameScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            Log.d(TAG, "gameScanCallback")
            when (callbackType) {
                ScanSettings.CALLBACK_TYPE_ALL_MATCHES -> {
                    onGameScanResult(result.device, result.rssi)
                }
                ScanSettings.CALLBACK_TYPE_MATCH_LOST -> {
                    Log.d(TAG, "lost " + result.device.name)
                    // when do we delete a device?
                }
            }
        }
    }

    init {
        bluetoothController.setContext(application.applicationContext)
    }

    fun addDevice(device: Device) {
        _devices[device] = System.currentTimeMillis()
    }

    fun contains(address: String): Boolean {
        return _devices.keys.any {d -> d.address == address}
    }

    fun findDevice(address: String): Device? {
        return _devices.keys.find { d -> d.address == address }
    }

    private fun getLastRssiMillis(device: Device): Long {
        return System.currentTimeMillis() - _devices[device]!!
    }

    /**
     * Store rssi of device, if the last read was x millis before.
     */
    fun onGameScanResult(bluetoothDevice: BluetoothDevice, rssi: Int) {
        if (!contains(bluetoothDevice.address)) _devices[Device(bluetoothDevice.address, -70, bluetoothDevice)] = System.currentTimeMillis()
        val device = findDevice(bluetoothDevice.address)!!
        val millisPassed = getLastRssiMillis(device)
        if (millisPassed > RSSI_READ_INTERVAL) {
            Log.d(TAG, "game scan: read rssi of ${device.address}, last read: $millisPassed")
            device.addRssi(rssi)
        }
    }


}