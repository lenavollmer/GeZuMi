package de.htw.gezumi.controller

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import de.htw.gezumi.gatt.GameService
import java.util.*
import kotlin.collections.ArrayList

private const val SCAN_PERIOD = 10000L
private const val TAG = "BTController"

class BluetoothController {

    private lateinit var _context: Context
    private lateinit var _bluetoothManager: BluetoothManager
    private val _bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter() // TODO clean up nullables and do proper bt support checking
    private val _bluetoothLeScanner: BluetoothLeScanner? = _bluetoothAdapter.bluetoothLeScanner

    private var scanning = false

    /**
     * Callback to receive information about the advertisement process.
     */
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.i(TAG, "LE Advertise Started.")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.w(TAG, "LE Advertise Failed: $errorCode")
        }
    }

    init {
        checkBluetoothSupport()
    }

    fun scanForDevices(leScanCallback: ScanCallback, serviceUUID: ParcelUuid) {
        val filter = ScanFilter.Builder()
                .setServiceUuid(serviceUUID)
                .build()
        val scanSettings = ScanSettings.Builder().setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES).build()

        _bluetoothLeScanner?.let { scanner ->
            if (!scanning) { // Stops scanning after a pre-defined scan period.
                Handler(Looper.getMainLooper()).postDelayed({
                    scanning = false
                    scanner.stopScan(leScanCallback)
                }, SCAN_PERIOD)
                scanning = true
                scanner.startScan(listOf(filter), scanSettings, leScanCallback)
                Log.d(TAG, "scanning ble")
            } else {
                scanning = false
                scanner.stopScan(leScanCallback)
            }
        }
    }

    /**
     * Begin advertising over Bluetooth that this device is connectable
     * and supports the Current Time Service.
     */
    fun startAdvertising(uuid: ParcelUuid) {
        require(::_bluetoothManager.isInitialized) {"Must have context set"}
        val bluetoothLeAdvertiser: BluetoothLeAdvertiser? =
            _bluetoothManager.adapter.bluetoothLeAdvertiser

        bluetoothLeAdvertiser?.let {
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build()

            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(uuid)
                .build()

            it.startAdvertising(settings, data, advertiseCallback)
        } ?: Log.w(TAG, "Failed to create advertiser")
        Log.d(TAG, "advertise started")
    }

    /**
     * Stop Bluetooth advertisements.
     */
    fun stopAdvertising() {
        Log.d(TAG, "stop advertising")
        val bluetoothLeAdvertiser: BluetoothLeAdvertiser? =
            _bluetoothManager.adapter.bluetoothLeAdvertiser
        bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback) ?: Log.w(TAG, "Failed to create advertiser")
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