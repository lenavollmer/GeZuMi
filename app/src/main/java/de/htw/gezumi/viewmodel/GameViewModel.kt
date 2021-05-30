package de.htw.gezumi.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import de.htw.gezumi.Utils
import de.htw.gezumi.callbacks.GameJoinUICallback
import de.htw.gezumi.controller.BluetoothController
import de.htw.gezumi.gatt.GameService
import de.htw.gezumi.gatt.GattClient
import de.htw.gezumi.model.Device
import de.htw.gezumi.model.Game
import de.htw.gezumi.util.FileStorage
import java.util.*

private const val TAG = "GameViewModel"
const val RSSI_READ_INTERVAL = 500
const val GAME_ID_LENGTH = 8 // 8 bytes for game id (4 prefix, 4 random)
const val RANDOM_GAME_ID_PART_LENGTH = 4
// remaining 13 bytes of advertise package for name and device id
const val GAME_NAME_LENGTH = 8 // don't forget to change edit_text limitation
const val DEVICE_ID_LENGTH = 5
const val DEVICE_ID_OFFSET = GAME_ID_LENGTH + GAME_NAME_LENGTH

class GameViewModel(application: Application) : AndroidViewModel(application) {

    private val myDeviceId = ByteArray(5)

    lateinit var gameJoinUICallback: GameJoinUICallback
    lateinit var hostScanCallback: ScanCallback
    lateinit var gattClient: GattClient

    val bluetoothController: BluetoothController = BluetoothController()
    private val _devices = mutableMapOf<Device, Long>()
    val devices: List<Device> get() = _devices.keys.toList()

    var host: Device? = null // is null for host themselves // is currently not the same object as host in _devices (and has default txpower)

    val game = Game()

    // the game id consists of a fixed host prefix (4 bytes) and a random id part (4 bytes)
    var gameId: ByteArray = ByteArray(0) // 21 bytes left for game attributes like game name etc.
        get() {
            require(field.size <= GAME_ID_LENGTH) {"Wrong game id"}
            return field + ByteArray(GAME_ID_LENGTH - field.size) // fill with zeros if
        }

    @kotlin.ExperimentalUnsignedTypes
    @SuppressLint("DefaultLocale")
    val gameScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            when (callbackType) {
                ScanSettings.CALLBACK_TYPE_ALL_MATCHES -> {
                    val deviceId = GameService.extractDeviceId(result)
                    onGameScanResult(deviceId, result.rssi, result.txPower, result.device)
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
        bluetoothController.myDeviceId = myDeviceId
    }

    @kotlin.ExperimentalUnsignedTypes
    @SuppressLint("DefaultLocale")
    fun onGameJoin() { // == approve
        Log.d(TAG, "on game join")
        gameJoinUICallback.gameJoined()
        // waiting for game start is not necessary
        Log.d(TAG, "start advertising on game id: $gameId")
        bluetoothController.startAdvertising(gameId)
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
        Log.d(TAG, "I'm in onGameStart: $game")
        gameJoinUICallback.gameStarted()
    }

    @kotlin.ExperimentalUnsignedTypes
    @SuppressLint("DefaultLocale")
    fun onGameLeave() {
        Log.d(TAG, "on game leave")
        // TODO host leaves game
        bluetoothController.stopAdvertising()
        bluetoothController.stopScan(gameScanCallback)
        game.resetState()
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
    @kotlin.ExperimentalUnsignedTypes
    @SuppressLint("DefaultLocale")
    fun onGameScanResult(deviceAddress: ByteArray, rssi: Int, txPower: Int, bluetoothDevice: BluetoothDevice) {
        // add device to game if necessary
        if (!Utils.contains(devices, deviceAddress))
            addDevice(Device(deviceAddress, txPower, bluetoothDevice))

        val device = Utils.findDevice(devices, deviceAddress)!!
        val millisPassed = getLastRssiMillis(device)
        if (millisPassed > RSSI_READ_INTERVAL) {
            Log.d(TAG, "game scan: read rssi of ${Utils.toHexString(deviceAddress)}, last read: $millisPassed")
            device.addRssi(rssi)
            if (!isHost())
                gattClient.sendPlayerUpdate(device.getDeviceData())
            // TODO: else isHost: call fun that processes data received from clients with own data
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