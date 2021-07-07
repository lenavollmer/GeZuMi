package de.htw.gezumi.controller

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import de.htw.gezumi.gatt.GameService
import de.htw.gezumi.viewmodel.*
import java.nio.ByteBuffer

const val HOST_SCAN_KEY = "host"
const val GAME_SCAN_KEY = "game"

class BluetoothController {

    private lateinit var _context: Context
    private lateinit var _bluetoothManager: BluetoothManager
    private val _bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
    private val _bluetoothLeScanner: BluetoothLeScanner? = _bluetoothAdapter.bluetoothLeScanner

    private val _scanSettings = ScanSettings.Builder().setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES).build()

    // we need a map of lists because scans might be started, but fail on start. it must be ensure, that all scans are stopped if we call stop
    private val _startedScans: MutableMap<String, MutableList<ScanCallback>>  = mutableMapOf(HOST_SCAN_KEY to mutableListOf(), GAME_SCAN_KEY to mutableListOf())

    private val _advertiseSettings: AdvertiseSettings = AdvertiseSettings.Builder()
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
        .setConnectable(true)
        .setTimeout(0)
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
        .build()

    lateinit var myDeviceId: ByteArray

    private val advertiseCallback = object : AdvertiseCallback() {}


    fun startHostScan(leScanCallback: ScanCallback) {
        stopScan(HOST_SCAN_KEY)
        // just scan for host prefix
        val ignore = ByteArray(RANDOM_GAME_ID_PART_LENGTH + GAME_NAME_LENGTH + DEVICE_ID_LENGTH + TXPOWER_LENGTH)
        val mask = byteArrayOf(1, 1, 1, 1) + ignore
        val filterBytes = GameService.HOST_ID_PREFIX + ignore

        val filterBuilder = ScanFilter.Builder()
        filterBuilder.setManufacturerData(76, filterBytes, mask)

        val filter = filterBuilder.build()
        _bluetoothLeScanner?.startScan(listOf(filter), _scanSettings, leScanCallback)
        _startedScans[HOST_SCAN_KEY]!!.add(leScanCallback)
    }

    @kotlin.ExperimentalUnsignedTypes
    @SuppressLint("DefaultLocale")
    fun startScan(leScanCallback: ScanCallback, gameId: ByteArray) {
        stopScan(GAME_SCAN_KEY)
        // scan for specific game with prefix and random part
        val ignore = ByteArray(GAME_NAME_LENGTH + DEVICE_ID_LENGTH + TXPOWER_LENGTH) // mask device address and game name
        val filterBytes = gameId + ignore
        val mask = ByteArray(GAME_ID_LENGTH) { 1 } + ignore
        mask[3] = 0 // ignore 4th byte to see host too

        val filterBuilder = ScanFilter.Builder()
        filterBuilder.setManufacturerData(76, filterBytes, mask)

        val filter = filterBuilder.build()
        _bluetoothLeScanner?.startScan(listOf(filter), _scanSettings, leScanCallback)
        _startedScans[GAME_SCAN_KEY]!!.add(leScanCallback)
    }

    /**
     * Stop the scan using the scan key constant.
     */
    fun stopScan(scanKey: String) {
        if (_startedScans[scanKey]!!.size == 0) {
            return
        }
        for (scanCallback in _startedScans[scanKey]!!){
            _bluetoothLeScanner?.stopScan(scanCallback)
        }
    }

    @kotlin.ExperimentalUnsignedTypes
    fun startAdvertising(gameId: ByteArray, name: ByteArray = ByteArray(0)) { // leave empty if client because name is not important then
        require(::_bluetoothManager.isInitialized) {"Must have context set"}
        val bluetoothLeAdvertiser: BluetoothLeAdvertiser? = _bluetoothManager.adapter.bluetoothLeAdvertiser

        // fill game name to full length
        val fullGameNameBytes = ByteArray(GAME_NAME_LENGTH)
        System.arraycopy(name, 0, fullGameNameBytes, 0, name.size)

        val manufacturerData = gameId + fullGameNameBytes + myDeviceId + ByteBuffer.allocate(2).putShort(GameViewModel.instance.txPower!!).array()
        val advertiseData = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(false)
            .addManufacturerData(76, manufacturerData)
            .build()

        bluetoothLeAdvertiser?.startAdvertising(_advertiseSettings, advertiseData, advertiseCallback)
    }

    fun openGattServer(gattServerCallback: BluetoothGattServerCallback): BluetoothGattServer {
        return _bluetoothManager.openGattServer(_context, gattServerCallback)
    }

    fun setContext(context: Context) {
        _context = context
        _bluetoothManager = _context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
}