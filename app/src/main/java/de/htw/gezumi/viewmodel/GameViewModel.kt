package de.htw.gezumi.viewmodel

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.graphics.Point
import android.os.ParcelUuid
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import de.htw.gezumi.callbacks.GameJoinUICallback
import de.htw.gezumi.controller.BluetoothController
import de.htw.gezumi.model.Device
import de.htw.gezumi.util.FileStorage
import java.util.*

private const val TAG = "GameViewModel"
const val RSSI_READ_INTERVAL = 500

class GameViewModel(application: Application) : AndroidViewModel(application) {


    var gameJoinUICallback: GameJoinUICallback? = null

    val bluetoothController: BluetoothController = BluetoothController()
    private val _devices = mutableMapOf<Device, Long>()
    val devices: Set<Device> get() = _devices.keys

    // Set numbers of players = currently fixed to three
    private val _players = 3
    val players: Int get() = _players

    // TODO time
    private val _playerLocations = MutableLiveData<List<Point>>(
        listOf(
            Point(100, 20),
            Point(35, 150),
            Point(70, 300)
        )
    )
    val playerLocations: LiveData<List<Point>> get() = _playerLocations

    private val _targetShape = generateGeometricObject(_players)
    val targetShape: List<Point> get() = _targetShape

    // Determines whether the target shape has been matched by the players
    private var _shapeMatched: Boolean = false
    val shapeMatched: Boolean get() = _shapeMatched

    lateinit var host: Device // is null for host themselves // is currently not the same object as host in _devices (and has default txpower)
    lateinit var gameId: UUID

    fun isJoined(): Boolean = ::gameId.isInitialized

    fun setPlayerLocations(locations: List<Point>) {
        _playerLocations.postValue(locations)
    }

    fun setShapeMatched(matchedShape: Boolean) {
        _shapeMatched = matchedShape
    }

    fun onGameJoin() {
        Log.d(TAG, "on game join")
        gameJoinUICallback?.gameJoined()
        // waiting for game start is not necessary
        Log.d(TAG, "start advertising on game id: $gameId")
        bluetoothController.startAdvertising(ParcelUuid(gameId))
        Log.d(TAG, "start scanning for players on game id: $gameId")
        bluetoothController.stopScan(hostScanCallback)
        bluetoothController.startScan(gameScanCallback, ParcelUuid(gameId))
    }

    fun onGameLeave() {
        Log.d(TAG, "on game leave")
        // TODO host leaves game
        bluetoothController.stopAdvertising()
        bluetoothController.stopScan(gameScanCallback)
        // Handler(Looper.getMainLooper()).post{}
    }

    fun onGameStart() {
        gameJoinUICallback?.gameStarted()
    }

    fun onGameDecline() {
        gameJoinUICallback?.gameDeclined()
    }

    private fun isHost(): Boolean = !::host.isInitialized

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

    lateinit var hostScanCallback: ScanCallback

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
        if (!contains(bluetoothDevice.address)) _devices[Device(bluetoothDevice.address, txPower, bluetoothDevice)] = System.currentTimeMillis()
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

    fun generateGeometricObject(players: Int): List<Point> {
        val generatedPoints = mutableListOf<Point>()
        for (i in 1..players) {
            generatedPoints.add(Point((0..250).random(), (0..400).random()))
        }

        return generatedPoints
    }


}