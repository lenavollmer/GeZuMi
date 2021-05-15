package de.htw.gezumi.viewmodel

import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.ViewModel
import de.htw.gezumi.model.Device

private const val TAG = "GameViewModel"
const val RSSI_READ_INTERVAL = 500

class GameViewModel : ViewModel() {

    private val _devices = mutableMapOf<Device, Long>()
    val devices: Set<Device> get() = _devices.keys

    lateinit var host: Device
    lateinit var gameId: String

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
            Log.d(TAG, "read rssi of ${device.address}, last read: $millisPassed")
            device.addRssi(rssi)
        }
    }
}