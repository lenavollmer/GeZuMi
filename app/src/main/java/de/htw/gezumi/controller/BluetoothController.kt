package de.htw.gezumi.controller

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import de.htw.gezumi.Utils
import java.nio.charset.Charset

private const val SCAN_PERIOD = 10000L
private const val SERVICE_UUID_MASK_STRING = "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFF0000"
private const val TAG = "BTController"

class BluetoothController {

    private lateinit var _context: Context
    private lateinit var _bluetoothManager: BluetoothManager
    private val _bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter() // TODO clean up nullables and do proper bt support checking
    private val _bluetoothLeScanner: BluetoothLeScanner? = _bluetoothAdapter.bluetoothLeScanner

    private var _scanFilters = mutableListOf<ScanFilter>()
    private val _scanSettings = ScanSettings.Builder().setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES).build()

    private val _advertiseSettings: AdvertiseSettings = AdvertiseSettings.Builder()
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
        .setConnectable(true)
        .setTimeout(0)
        .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
        .build()

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.d(TAG, "ble advertise started")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.d(TAG, "ble advertise failed: $errorCode")
        }
    }

    init {
        checkBluetoothSupport()
    }

    @kotlin.ExperimentalUnsignedTypes
    @SuppressLint("DefaultLocale")
    fun startScan(leScanCallback: ScanCallback, filterBytes: ByteArray, masked: Boolean = false) {
        val mask = byteArrayOf(1, 1, 1, 1, 0, 0, 0, 0, 0/*, 0, 0, 0, 0, 0, 0, 0*/)

        Log.d(TAG, "" + Utils.toHexString(filterBytes))
        Log.d(TAG, "SCAN SIZE:        ${filterBytes.size}")

        val filterBuilder = ScanFilter.Builder()
        if (masked)
            filterBuilder.setManufacturerData(76, filterBytes, mask)
        else
            filterBuilder.setManufacturerData(76, filterBytes)

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
    fun startAdvertising(gameId: ByteArray) {
        require(::_bluetoothManager.isInitialized) {"Must have context set"}
        val bluetoothLeAdvertiser: BluetoothLeAdvertiser? = _bluetoothManager.adapter.bluetoothLeAdvertiser
        val advertiseData = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(true)
            .addManufacturerData(76, gameId)
            .build()
        val size = gameId.size
        Log.d(TAG, "$gameId SIZE:        $size")
        Log.d(TAG, "" + Utils.toHexString(gameId))
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

    /**
     * Verify the level of Bluetooth support provided by the hardware.
     * @return true if Bluetooth is properly supported, false otherwise.
     */
    private fun checkBluetoothSupport(): Boolean {

        /* Disabled since currently BluetoothAdapter is forced
        if (_bluetoothAdapter == null) {
            Log.w(TAG, "Bluetooth is not supported")
            return false
        } */
        //!_connectionFragment.requireContext().packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)
        if (_bluetoothLeScanner == null) {
            Log.w(TAG, "Bluetooth LE is not supported")
            return false
        }
        return true
    }

    fun setContext(context: Context) {
        _context = context
        _bluetoothManager = _context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }
}