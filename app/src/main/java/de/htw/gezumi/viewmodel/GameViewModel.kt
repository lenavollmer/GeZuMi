package de.htw.gezumi.viewmodel

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import de.htw.gezumi.Utils
import de.htw.gezumi.adapter.ApprovedDevicesAdapter
import de.htw.gezumi.calculation.Conversions
import de.htw.gezumi.calculation.Vec
import de.htw.gezumi.callbacks.GameJoinUICallback
import de.htw.gezumi.callbacks.PlayerUpdateCallback
import de.htw.gezumi.controller.BluetoothController
import de.htw.gezumi.controller.GAME_SCAN_KEY
import de.htw.gezumi.controller.HOST_SCAN_KEY
import de.htw.gezumi.gatt.GameService
import de.htw.gezumi.gatt.GattClient
import de.htw.gezumi.gatt.GattServer
import de.htw.gezumi.model.Device
import de.htw.gezumi.model.DeviceData
import de.htw.gezumi.model.Game
import de.htw.gezumi.util.FileStorage
import java.util.*

private const val TAG = "GameViewModel"
const val RSSI_READ_INTERVAL = 500
const val GAME_ID_LENGTH = 8 // 8 bytes for game id (4 prefix, 4 random)
const val RANDOM_GAME_ID_PART_LENGTH = 4

// remaining 13 bytes of advertise package for name and device id
const val GAME_NAME_LENGTH = 8 // don't forget to change edit_text limitation
const val DEVICE_ID_LENGTH = 3
const val DEVICE_ID_OFFSET = GAME_ID_LENGTH + GAME_NAME_LENGTH

class GameViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        lateinit var instance: GameViewModel
    }

    val myDeviceId = ByteArray(3)

    lateinit var gameJoinUICallback: GameJoinUICallback
    lateinit var hostScanCallback: ScanCallback
    lateinit var gattClient: GattClient
    lateinit var gattServer: GattServer

    val bluetoothController: BluetoothController = BluetoothController()
    val devices = mutableListOf<Device>()

    private lateinit var _distances: Array<FloatArray>
    private var _positions: List<Vec> = listOf()

    var host: Device? = null // is null for host themselves // is currently not the same object as host in _devices (and has default txpower)

    val game = Game()

    // the game id consists of a fixed host prefix (4 bytes) and a random id part (4 bytes)
    var gameId: ByteArray = ByteArray(0) // 21 bytes left for game attributes like game name etc.
        get() {
            require(field.size <= GAME_ID_LENGTH) { "Wrong game id" }
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
                    val playerName = GameService.extractGameName(result)
                    onGameScanResult(deviceId, playerName, result.rssi, result.txPower)
                }
                ScanSettings.CALLBACK_TYPE_MATCH_LOST -> {
                    Log.d(TAG, "lost " + result.device.name)
                    // when do we delete a device?
                }
            }
        }
    }


    @kotlin.ExperimentalUnsignedTypes
    @SuppressLint("DefaultLocale")
    val playerUpdateCallback: PlayerUpdateCallback = object : PlayerUpdateCallback {
        /**
         * Fill distance matrix, calculate positions, send host updates with changed positions.
         * Only called on the host device.
         */
        override fun onPlayerUpdate(deviceData: DeviceData) {
            var senderDeviceIdx = Utils.findDeviceIndex(devices, deviceData.senderId)
            var deviceDistanceToIdx = Utils.findDeviceIndex(devices, deviceData.deviceId)
            if (deviceData.deviceId contentEquals myDeviceId) {
                deviceDistanceToIdx = devices.size // another device measured distance to myself
            }
            else if (deviceData.senderId contentEquals myDeviceId) {
                senderDeviceIdx = devices.size // the measurement is by myself (the host)
            }

            // device not present yet
            if (senderDeviceIdx == -1 || deviceDistanceToIdx == -1) return

            _distances[senderDeviceIdx][deviceDistanceToIdx] = deviceData.values[0]

            val newPositions = Conversions.distancesToPoints(_distances)

            // if contains new player, take all new
            val changedPositionsIndices = if(newPositions.size > _positions.size) newPositions.indices else newPositions.indices.filter {
                newPositions[it] != _positions[it]
            }

            changedPositionsIndices.forEach {
                val deviceId = if (it == devices.size) myDeviceId else devices[it].deviceId
                gattServer.notifyHostUpdate(
                    DeviceData(
                        deviceId,
                        myDeviceId,
                        floatArrayOf(newPositions[it].x, newPositions[it].y)
                    )
                )
                // also update own game
                game.updatePlayer(deviceData.deviceId, Vec(newPositions[it].x, newPositions[it].y))
            }
            _positions = newPositions
        }

    }


    init {
        // singleton
        instance = this
        // generate random id
        Random().nextBytes(myDeviceId)
        bluetoothController.setContext(application.applicationContext)
        bluetoothController.myDeviceId = myDeviceId
    }

    @kotlin.ExperimentalUnsignedTypes
    @SuppressLint("DefaultLocale")
    fun onGameJoin() { // == approve
        Log.d(TAG, "on game join")
        gameJoinUICallback.gameJoined()
        // waiting for game start is not necessary
        bluetoothController.startAdvertising(gameId, "gustav".toByteArray(Charsets.UTF_8))
        bluetoothController.stopScan(HOST_SCAN_KEY)
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

    @kotlin.ExperimentalUnsignedTypes
    @SuppressLint("DefaultLocale")
    fun onGameLeave() {
        Log.d(TAG, "on game leave")
        bluetoothController.stopAdvertising()
        bluetoothController.stopScan(GAME_SCAN_KEY)
        gameJoinUICallback.gameLeft()
        // TODO: test game leave and join new game
        // TODO: go back to join activity if game already started
        // TODO: differentiate between game left and game terminated by host (terminated by host does not work)
        // Handler(Looper.getMainLooper()).post{}
        clearModel()
    }

    fun clearModel() {
        // TODO lifecycle: add stuff here
        devices.clear()
        game.clear()
    }

    fun isJoined(): Boolean = gameId.isNotEmpty()

    private fun isHost(): Boolean = host == null

    private var _playerListAdapter: ApprovedDevicesAdapter? = null

    fun setPlayerListAdapter(playerListAdapter: ApprovedDevicesAdapter) {
        _playerListAdapter = playerListAdapter
    }

    @kotlin.ExperimentalUnsignedTypes
    fun addDevice(device: Device) {
        devices.add(device)
        // also add player and set name to address until it gets updated
        game.addPlayerIfNew(device.deviceId)
        game.getPlayer(device.deviceId)!!.setName(Utils.toHexString(device.deviceId))
        // extend distance matrix for new player
        _distances = Array(devices.size + 1) { FloatArray(devices.size + 1) } // +1 for self
        // refresh host screen, if is host
        Handler(Looper.getMainLooper()).post{ _playerListAdapter?.notifyDataSetChanged() } // TODO: use intent here
    }

    private fun getLastRssiMillis(device: Device): Long {
        return System.currentTimeMillis() - device.lastSeen
    }

    /**
     * Store rssi of device, if last read x millis before.
     * Send a player update to the host.
     */
    @kotlin.ExperimentalUnsignedTypes
    @SuppressLint("DefaultLocale")
    fun onGameScanResult(deviceId: ByteArray, playerName: String, rssi: Int, txPower: Int) {
        var device = Utils.findDevice(devices, deviceId)
        // add device if not present yet
        if (device == null) {
            addDevice(Device(deviceId, txPower, null))
            device = Utils.findDevice(devices, deviceId)!!
        }
        else if (device.txPower == 0) // if devices was added at identification, then fill txpower
            device.txPower = txPower

        // update player name
        game.getPlayer(deviceId)!!.setName(playerName)

        // store rssi and send player update
        val millisPassed = getLastRssiMillis(device)

        if (millisPassed > RSSI_READ_INTERVAL) {
            Log.d(TAG, "game scan: read rssi of ${Utils.toHexString(deviceId)}, last read: $millisPassed")
            device.addRssi(rssi)
            val deviceData = DeviceData.fromDevice(device, myDeviceId)
            if (!isHost())
                gattClient.sendPlayerUpdate(deviceData)
            else // also take own data in account
                playerUpdateCallback.onPlayerUpdate(deviceData)
            device.lastSeen = System.currentTimeMillis()
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