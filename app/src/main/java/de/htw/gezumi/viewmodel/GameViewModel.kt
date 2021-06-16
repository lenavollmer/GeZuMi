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
import de.htw.gezumi.calculation.Geometry
import de.htw.gezumi.calculation.Vec
import de.htw.gezumi.callbacks.GameJoinUICallback
import de.htw.gezumi.callbacks.GameLeaveUICallback
import de.htw.gezumi.callbacks.PlayerUpdateCallback
import de.htw.gezumi.controller.BluetoothController
import de.htw.gezumi.controller.GAME_SCAN_KEY
import de.htw.gezumi.controller.HOST_SCAN_KEY
import de.htw.gezumi.gatt.GameService
import de.htw.gezumi.gatt.GattClient
import de.htw.gezumi.gatt.GattServer
import de.htw.gezumi.model.BluetoothData
import de.htw.gezumi.model.Device
import de.htw.gezumi.model.Game
import de.htw.gezumi.util.Constants.TARGET_SHAPE_ID
import de.htw.gezumi.util.FileStorage
import java.util.*

private const val TAG = "GameViewModel"
const val RSSI_READ_INTERVAL = 500
// total of 21 (24 without standard txPowerLevel?) bytes available for custom data in advertise packet
const val GAME_ID_LENGTH = 8 // 8 bytes for game id (4 prefix, 4 random)
const val RANDOM_GAME_ID_PART_LENGTH = 4

// remaining 13 bytes of advertise package for name and device id
const val GAME_NAME_LENGTH = 8 // don't forget to change edit_text limitation
const val DEVICE_ID_LENGTH = 3
const val TXPOWER_LENGTH = 2 // txpower is a short
const val DEVICE_ID_OFFSET = GAME_ID_LENGTH + GAME_NAME_LENGTH
const val TXPOWER_OFFSET = DEVICE_ID_OFFSET + DEVICE_ID_LENGTH

class GameViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        lateinit var instance: GameViewModel
    }

    val myDeviceId = ByteArray(3)
    var playerName: String? = null
    var txPower: Short? = null

    lateinit var gameJoinUICallback: GameJoinUICallback
    var gameLeaveUICallback: GameLeaveUICallback? = null
    lateinit var hostScanCallback: ScanCallback
    lateinit var gattClient: GattClient
    lateinit var gattServer: GattServer
    fun isGattServerInitialized() = this::gattServer.isInitialized
    fun isGattClientInitialized() = this::gattClient.isInitialized

    val bluetoothController: BluetoothController = BluetoothController()
    val devices = mutableListOf<Device>()

    private lateinit var _distances: Array<FloatArray>
    private var _positions: List<Vec> = listOf()

    var host: Device? =
        null // is null for host themselves // is currently not the same object as host in _devices (and has default txpower)

    val game = Game(host?.deviceId)

    // the game id consists of a fixed host prefix (4 bytes) and a random id part (4 bytes)
    var gameId: ByteArray = ByteArray(0)
        get() {
            require(field.size <= GAME_ID_LENGTH) { "Wrong game id" }
            return field + ByteArray(GAME_ID_LENGTH - field.size) // fill with zeros if
        }

    fun makeGameId() {
        GameService.newRandomId()
        gameId = GameService.HOST_ID_PREFIX + GameService.randomIdPart
    }

    @kotlin.ExperimentalUnsignedTypes
    @SuppressLint("DefaultLocale")
    val gameScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            when (callbackType) {
                ScanSettings.CALLBACK_TYPE_ALL_MATCHES -> {
                    val deviceId = GameService.extractDeviceId(result)
                    val playerName = GameService.extractName(result)
                    val txPower = GameService.extractTxPower(result)
                    onGameScanResult(deviceId, playerName, result.rssi, txPower)
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
        override fun onPlayerUpdate(bluetoothData: BluetoothData) {
            var senderDeviceIdx = Utils.findDeviceIndex(devices, bluetoothData.senderId)
            var deviceDistanceToIdx = Utils.findDeviceIndex(devices, bluetoothData.id)
            if (bluetoothData.id contentEquals myDeviceId) {
                deviceDistanceToIdx = devices.size // another device measured distance to myself
            } else if (bluetoothData.senderId contentEquals myDeviceId) {
                senderDeviceIdx = devices.size // the measurement is by myself (the host)
            }

            // device not present yet
            if (senderDeviceIdx == -1 || deviceDistanceToIdx == -1) return

            _distances[senderDeviceIdx][deviceDistanceToIdx] = bluetoothData.values[0]

            val newPositions = Conversions.distancesToPoints(_distances)

            // if contains new player, take all new
            val changedPositionsIndices =
                if (newPositions.size > _positions.size) newPositions.indices else newPositions.indices.filter {
                    newPositions[it] != _positions[it]
                }

            changedPositionsIndices.forEach {
                val deviceId = if (it == devices.size) myDeviceId else devices[it].deviceId
                gattServer.notifyHostUpdate(
                    BluetoothData(
                        deviceId,
                        myDeviceId,
                        floatArrayOf(newPositions[it].x, newPositions[it].y)
                    )
                )
                // also update own game
                game.updatePlayer(deviceId, Vec(newPositions[it].x, newPositions[it].y))
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
        bluetoothController.startAdvertising(gameId, if (playerName != null) playerName!!.toByteArray(Charsets.UTF_8) else ByteArray(0))
        bluetoothController.startScan(gameScanCallback, gameId)
    }

    fun onGameDecline() {
        host = null
        gattClient.disconnect()
        gameJoinUICallback.gameDeclined()
    }

    fun onGameStart() {
        Log.d(TAG, "I'm in onGameStart: $game")
        bluetoothController.stopScan(HOST_SCAN_KEY)
        gameJoinUICallback.gameStarted()
    }

    @kotlin.ExperimentalUnsignedTypes
    @SuppressLint("DefaultLocale")
    fun onGameLeave() {
        Log.d(TAG, "on game leave")
        bluetoothController.stopAdvertising()
        bluetoothController.stopScan(GAME_SCAN_KEY)
        Handler(Looper.getMainLooper()).post{
            game.resetState()
        }
        if (gameLeaveUICallback != null) gameLeaveUICallback?.gameLeft() // it crashes otherwise
        // TODO: test game leave and join new game
        // TODO: go back to join activity if game already started
        // TODO: differentiate between game left and game terminated by host (terminated by host does not work)
        // Handler(Looper.getMainLooper()).post{}
        clearModel()
        if (isHost()) {
            // restart advertising on a new game id
            makeGameId()
            bluetoothController.startAdvertising(gameId, GameService.gameName.toByteArray(Charsets.UTF_8))
            bluetoothController.startScan(gameScanCallback, gameId)
        }
    }

    @kotlin.ExperimentalUnsignedTypes
    fun onPlayerNameChanged(newName: String) {
        require(newName.length <= GAME_NAME_LENGTH) { "Player name too long" }
        playerName = newName
        // restart advertisement with new name if client
        if (!isHost()) {
            bluetoothController.stopAdvertising()
            bluetoothController.startAdvertising(gameId, if (playerName != null) playerName!!.toByteArray(Charsets.UTF_8) else ByteArray(0))
        }
    }

    fun clearModel() {
        // TODO lifecycle: add stuff here
        devices.clear()
        game.clear()
        playerName = null
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
        Handler(Looper.getMainLooper()).post { _playerListAdapter?.notifyDataSetChanged() } // TODO: use intent here
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
    fun onGameScanResult(deviceId: ByteArray, playerName: String, rssi: Int, txPower: Short) {
        var device = Utils.findDevice(devices, deviceId)
        // add device if not present yet
        if (device == null) {
            addDevice(Device(deviceId, txPower, null))
            device = Utils.findDevice(devices, deviceId)!!
        } else if (device.txPower == 0.toShort()) { // if devices was added at identification, then fill txpower
            device.txPower = txPower
            Log.d(TAG, "game scan: set txPower for device: $txPower")
        }

        // update player name
        if (playerName.isNotBlank())
            game.getPlayer(deviceId)!!.setName(playerName)
        else // set back to device id if player deletes name
            game.getPlayer(deviceId)!!.setName(Utils.toHexString(deviceId))

        // store rssi and send player update
        val millisPassed = getLastRssiMillis(device)

        if (millisPassed > RSSI_READ_INTERVAL) {
            Log.d(TAG, "game scan: read rssi of ${Utils.toHexString(deviceId)}, last read: $millisPassed")
            device.addRssi(rssi)
            val deviceData = BluetoothData.fromDevice(device, myDeviceId)
            if (!isHost())
                gattClient.sendPlayerUpdate(deviceData)
            else // also take own data in account
                playerUpdateCallback.onPlayerUpdate(deviceData)
            device.lastSeen = System.currentTimeMillis()
        }
    }

    fun writeRSSILog() {
        FileStorage.writeFile(
            getApplication<Application>().applicationContext,
            "${Calendar.getInstance().time}_distance_log.txt",
            devices.iterator().next().rssiHistory.toString()
        )
    }

    @kotlin.ExperimentalUnsignedTypes
    fun updateTargetShape() {
        if (host == null) {
            Log.d(TAG, "generating target shapes for ${devices.size + 1} players" )
            val targetShape = Geometry.generateGeometricObject(devices.size + 1)
            game.setTargetShape(targetShape as MutableList<Vec>)
            targetShape.forEach {
                gattServer.indicateHostUpdate(
                    BluetoothData(
                        TARGET_SHAPE_ID,
                        myDeviceId,
                        floatArrayOf(it.x, it.y)
                    )
                )
            }
        }
    }
}