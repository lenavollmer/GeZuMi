package de.htw.gezumi.controller

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.util.Log
import de.htw.gezumi.Utils
import de.htw.gezumi.gatt.GameService
import de.htw.gezumi.viewmodel.DEVICE_ID_LENGTH
import de.htw.gezumi.viewmodel.GAME_ID_LENGTH
import de.htw.gezumi.viewmodel.GAME_NAME_LENGTH
import de.htw.gezumi.viewmodel.RANDOM_GAME_ID_PART_LENGTH

private const val SCAN_PERIOD = 10000L
private const val SERVICE_UUID_MASK_STRING = "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFF0000"
private const val TAG = "BTController"

class BluetoothController {

    private lateinit var _context: Context
    private lateinit var _bluetoothManager: BluetoothManager
    private val _bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter() // TODO clean up nullables and do proper bt support checking
    private val _bluetoothLeScanner: BluetoothLeScanner? = _bluetoothAdapter.bluetoothLeScanner

    private var _scanFilters = mutableListOf<ScanFilter>()
    private val _scanSettings = ScanSettings.Builder()
        .setCallbackType(ScanSettings.CALLBACK_TYPE_FIRST_MATCH or ScanSettings.CALLBACK_TYPE_MATCH_LOST)
        .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()

    private val _advertiseSettings: AdvertiseSettings = AdvertiseSettings.Builder()
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
        .setConnectable(true)
        .setTimeout(0)
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
        .build()

    lateinit var myDeviceId: ByteArray

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.d(TAG, "ble advertise started")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.d(TAG, "ble advertise failed: $errorCode")
        }
    }

    init {
        enableBluetooth()
    }

    fun startHostScan(leScanCallback: ScanCallback) {
        // just scan for host prefix
        val ignore = ByteArray(RANDOM_GAME_ID_PART_LENGTH + GAME_NAME_LENGTH + DEVICE_ID_LENGTH)
        val mask = byteArrayOf(1, 1, 1, 1) + ignore
        val filterBytes = GameService.GAME_ID_PREFIX + ignore

        val filterBuilder = ScanFilter.Builder()
        filterBuilder.setManufacturerData(76, filterBytes, mask)

        val filter = filterBuilder.build()
        if (!_scanFilters.contains(filter)) _scanFilters.add(filter)
        Log.d(TAG, "start scanning for hosts")
        _bluetoothLeScanner?.startScan(_scanFilters, _scanSettings, leScanCallback)
    }

    @kotlin.ExperimentalUnsignedTypes
    @SuppressLint("DefaultLocale")
    fun startScan(leScanCallback: ScanCallback, gameId: ByteArray) {
        // scan for specific game with prefix and random part
        val ignore = ByteArray(GAME_NAME_LENGTH + DEVICE_ID_LENGTH) // mask device address and game name
        val filterBytes = gameId + ignore
        var mask = ByteArray(GAME_ID_LENGTH) { 1 } + ignore

        Log.d(TAG, "" + Utils.toHexString(filterBytes))
        Log.d(TAG, "SCAN SIZE:        ${filterBytes.size}")

        val filterBuilder = ScanFilter.Builder()
        filterBuilder.setManufacturerData(76, filterBytes, mask)

        val filter = filterBuilder.build()
        if (!_scanFilters.contains(filter)) _scanFilters.add(filter)
        Log.d(TAG, "start ble scanning")
        _bluetoothLeScanner?.startScan(_scanFilters, _scanSettings, leScanCallback)
    }

    fun stopScan(leScanCallback: ScanCallback) {
        _bluetoothLeScanner?.stopScan(leScanCallback)
    }

    @kotlin.ExperimentalUnsignedTypes
    @SuppressLint("DefaultLocale")
    fun startAdvertising(gameId: ByteArray, gameName: ByteArray = ByteArray(0)) { // leave empty if client because name is not important then
        require(::_bluetoothManager.isInitialized) {"Must have context set"}
        val bluetoothLeAdvertiser: BluetoothLeAdvertiser? = _bluetoothManager.adapter.bluetoothLeAdvertiser

        // fill game name to full length
        val fullGameNameBytes = ByteArray(GAME_NAME_LENGTH)
        System.arraycopy(gameName, 0, fullGameNameBytes, 0, gameName.size)

        val advertiseData = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(true)
            .addManufacturerData(76, gameId + fullGameNameBytes + myDeviceId)
            .build()

        val size = (gameId + fullGameNameBytes + myDeviceId).size
        Log.d(TAG, "${Utils.toHexString(gameId + fullGameNameBytes + myDeviceId)} SIZE:        $size")
        bluetoothLeAdvertiser?.startAdvertising(_advertiseSettings, advertiseData, advertiseCallback)
        ?: Log.d(TAG, "advertise failed")
    }

    fun stopAdvertising() {
        Log.d(TAG, "stop advertising")
        val bluetoothLeAdvertiser: BluetoothLeAdvertiser? = _bluetoothManager.adapter.bluetoothLeAdvertiser
        bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback) ?: Log.d(TAG, "stop advertise failed")
    }

    fun openGattServer(gattServerCallback: BluetoothGattServerCallback): BluetoothGattServer {
        return _bluetoothManager.openGattServer(_context, gattServerCallback)
    }

    fun enableBluetooth() {
        _bluetoothAdapter.enable()
    }

    fun isBluetoothEnabled(): Boolean {
        return _bluetoothAdapter.isEnabled
    }

    fun setContext(context: Context) {
        _context = context
        _bluetoothManager = _context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
}