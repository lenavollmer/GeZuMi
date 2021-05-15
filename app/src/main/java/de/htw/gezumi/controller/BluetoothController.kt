package de.htw.gezumi.controller

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.ParcelUuid
import android.util.Log
import java.util.*

private const val SCAN_PERIOD = 10000L
private const val TAG = "BTController"

class BluetoothController {

    private lateinit var _context: Context
    private lateinit var _bluetoothManager: BluetoothManager
    private val _bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter() // TODO clean up nullables and do proper bt support checking
    private val _bluetoothLeScanner: BluetoothLeScanner? = _bluetoothAdapter.bluetoothLeScanner

    private var _scanFilters = mutableListOf<ScanFilter>()
    private val _scanSettings = ScanSettings.Builder().setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES).build()

    val _advertiseSettings = AdvertiseSettings.Builder()
        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
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

    fun startScan(leScanCallback: ScanCallback, serviceUUID: ParcelUuid) {
        val filter = ScanFilter.Builder()
                .setServiceUuid(serviceUUID)
                .build()
        if (!_scanFilters.contains(filter)) _scanFilters.add(filter)

        Log.d(TAG, "start ble scanning")
        _bluetoothLeScanner?.startScan(_scanFilters, _scanSettings, leScanCallback)
    }

    /**
     * Stop scanning for the specified uuid.
     * @param leScanCallback if another scan is still running, leScanCallback has to be passed again
     */
    fun stopScan(leScanCallback: ScanCallback, serviceUUID: ParcelUuid) {
        val filter = ScanFilter.Builder()
            .setServiceUuid(serviceUUID)
            .build()
        require(_scanFilters.contains(filter)) { "Filter not present in scan filters" }
        _scanFilters.remove(filter)
        _bluetoothLeScanner?.stopScan(leScanCallback)
        if (_scanFilters.isNotEmpty())
            _bluetoothLeScanner?.startScan(_scanFilters, _scanSettings, leScanCallback)
    }

    fun startAdvertising(uuid: ParcelUuid, data: ByteArray = ByteArray(0)) {
        require(::_bluetoothManager.isInitialized) {"Must have context set"}
        val bluetoothLeAdvertiser: BluetoothLeAdvertiser? = _bluetoothManager.adapter.bluetoothLeAdvertiser
        val advertiseData = AdvertiseData.Builder()
            .setIncludeDeviceName(false)
            .setIncludeTxPowerLevel(true) // TODO include??

        if (data.isEmpty())
            advertiseData.addServiceUuid(uuid)
        else
            advertiseData.addServiceData(uuid, data)
        bluetoothLeAdvertiser?.startAdvertising(_advertiseSettings, advertiseData.build(), advertiseCallback)
        ?: Log.d(TAG, "advertise failed")
    }

    /**
     * Stop Bluetooth advertisements.
     */
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

        if (_bluetoothAdapter == null) {
            Log.w(TAG, "Bluetooth is not supported")
            return false
        }
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