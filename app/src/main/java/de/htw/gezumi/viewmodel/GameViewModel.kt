package de.htw.gezumi.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.util.Log
import android.view.Gravity
import androidx.lifecycle.AndroidViewModel
import de.htw.gezumi.Utils
import de.htw.gezumi.callbacks.GameJoinUICallback
import de.htw.gezumi.controller.BluetoothController
import de.htw.gezumi.gatt.GattClient
import de.htw.gezumi.gatt.GattClientCallback
import de.htw.gezumi.model.Device
import de.htw.gezumi.util.FileStorage
import java.util.*

private const val TAG = "GameViewModel"
const val RSSI_READ_INTERVAL = 500
const val GAME_ID_LENGTH: Int = 16 // 16 bytes for game id, then remaining 5 bytes for device id
const val DEVICE_ID_LENGTH: Int = 5

class GameViewModel(application: Application) : AndroidViewModel(application) {

    val myDeviceId = ByteArray(5)

    lateinit var gameJoinUICallback: GameJoinUICallback
    lateinit var hostScanCallback: ScanCallback
    lateinit var gattClient: GattClient

    val bluetoothController: BluetoothController = BluetoothController()
    private val _devices = mutableMapOf<Device, Long>()
    val devices: List<Device> get() = _devices.keys.toList()

    var host: Device? = null // is null for host themselves // is currently not the same object as host in _devices (and has default txpower)
    var gameId: ByteArray = ByteArray(0) // 21 bytes left for game attributes like game name etc.
        get() {
            require(field.size <= GAME_ID_LENGTH) {"Wrong game id"}
            return field + ByteArray(GAME_ID_LENGTH - field.size)
        }

    val gameScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            when (callbackType) {
                ScanSettings.CALLBACK_TYPE_ALL_MATCHES -> {
                    val deviceAddress = result.scanRecord!!.getManufacturerSpecificData(76)!!.sliceArray(GAME_ID_LENGTH until GAME_ID_LENGTH + 5)
                    onGameScanResult(deviceAddress, result.rssi, result.txPower, result.device)
                }
                ScanSettings.CALLBACK_TYPE_MATCH_LOST -> {
                    Log.d(TAG, "lost " + result.device.name)
                    // when do we delete a device?
                }
            }
        }
    }

    init {
        // generate random id
        Random().nextBytes(myDeviceId)
    }

    @kotlin.ExperimentalUnsignedTypes
    @SuppressLint("DefaultLocale")
    fun onGameJoin() { // == approve
        Log.d(TAG, "on game join")
        gameJoinUICallback.gameJoined()
        // waiting for game start is not necessary
        Log.d(TAG, "start advertising on game id: $gameId")
        bluetoothController.startAdvertising(gameId, myDeviceId)
        Log.d(TAG, "start scanning for players on game id: $gameId")
        bluetoothController.stopScan(hostScanCallback)
        bluetoothController.startScan(gameScanCallback, gameId)
    }

    fun onGameDecline() {
        host = null
        gattClient.disconnect()
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

    fun isJoined(): Boolean = gameId.isNotEmpty()

    private fun isHost(): Boolean = host == null

    init {
        bluetoothController.setContext(application.applicationContext)
    }

    private fun addDevice(device: Device) {
        _devices[device] = System.currentTimeMillis()
    }

    private fun getLastRssiMillis(device: Device): Long {
        return System.currentTimeMillis() - _devices[device]!!
    }

    /**
     * Store rssi of device, if last read x millis before.
     * Send a player update to the host.
     */
    fun onGameScanResult(deviceAddress: ByteArray, rssi: Int, txPower: Int, bluetoothDevice: BluetoothDevice) {
        // add device to game if necessary
        if (!Utils.contains(devices, deviceAddress))
            addDevice(Device(deviceAddress, txPower, bluetoothDevice))

        val device = Utils.findDevice(devices, deviceAddress)!!
        val millisPassed = getLastRssiMillis(device)
        if (millisPassed > RSSI_READ_INTERVAL) {
            Log.d(TAG, "game scan: read rssi of ${device.address}, last read: $millisPassed")
            device.addRssi(rssi)
            if (!isHost())
                gattClient.sendPlayerUpdate(device.getDeviceData())
            // TODO: else: call fun that processes data received from clients with own data
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