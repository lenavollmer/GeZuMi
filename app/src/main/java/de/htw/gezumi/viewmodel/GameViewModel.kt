package de.htw.gezumi.viewmodel

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.ParcelUuid
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import de.htw.gezumi.callbacks.GameJoinUICallback
import de.htw.gezumi.controller.BluetoothController
import de.htw.gezumi.model.Device
import de.htw.gezumi.util.FileStorage
import java.util.*

private const val TAG = "GameViewModel"
const val RSSI_READ_INTERVAL = 500

class GameViewModel(application: Application) : AndroidViewModel(application) {


    lateinit var gameJoinUICallback: GameJoinUICallback
    lateinit var hostScanCallback: ScanCallback

    val bluetoothController: BluetoothController = BluetoothController()
    private val _devices = mutableMapOf<Device, Long>()
    val devices: Set<Device> get() = _devices.keys

    lateinit var host: Device // is null for host themselves // is currently not the same object as host in _devices (and has default txpower)
    lateinit var gameId: UUID

    val gameScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            Log.d(TAG, "gameScanCallback, ${result.device.address}")
            when (callbackType) {
                ScanSettings.CALLBACK_TYPE_ALL_MATCHES -> {
                    onGameScanResult(result.device, result.rssi, result.txPower)
                }
                ScanSettings.CALLBACK_TYPE_MATCH_LOST -> {
                    Log.d(TAG, "lost " + result.device.name)
                    // when do we delete a device?
                }
            }
        }
    }

    fun onGameJoin() { // == approve
        Log.d(TAG, "on game join")
        gameJoinUICallback.gameJoined()
        // waiting for game start is not necessary
        Log.d(TAG, "start advertising on game id: $gameId")
        bluetoothController.startAdvertising(ParcelUuid(gameId))
        Log.d(TAG, "start scanning for players on game id: $gameId")
        bluetoothController.stopScan(hostScanCallback)
        bluetoothController.startScan(gameScanCallback, ParcelUuid(gameId))
    }

    fun onGameDecline() {
        gameJoinUICallback.gameDeclined()
    }

    fun onGameStart() {
        gameJoinUICallback.gameStarted()
    }

    fun onGameLeave() {
        Log.d(TAG, "on game leave")
        // TODO host leaves game
        bluetoothController.stopAdvertising()
        bluetoothController.stopScan(gameScanCallback)
        // Handler(Looper.getMainLooper()).post{}
    }

    fun isJoined(): Boolean = ::gameId.isInitialized

    fun isHost(): Boolean = !::host.isInitialized

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
    fun onGameScanResult(bluetoothDevice: BluetoothDevice, rssi: Int, txPower: Int) {
        if (!contains(bluetoothDevice.address))
            addDevice(Device(bluetoothDevice.address, txPower, bluetoothDevice))
        val device = findDevice(bluetoothDevice.address)!!
        val millisPassed = getLastRssiMillis(device)
        if (millisPassed > RSSI_READ_INTERVAL) {
            Log.d(TAG, "game scan: read rssi of ${device.address}, last read: $millisPassed")
            device.addRssi(rssi)
            _devices[device] = System.currentTimeMillis()
        }
    }

    // Todo currently broken
    fun writeRSSILog() {
        FileStorage.writeFile(
            getApplication<Application>().applicationContext,
            "${Calendar.getInstance().time}_distance_log.txt",
            devices.iterator().next().rssiHistory.toString()
        )
    }


}