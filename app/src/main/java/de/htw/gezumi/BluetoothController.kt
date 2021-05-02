package de.htw.gezumi

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.pm.PackageManager
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import kotlin.collections.ArrayList

private const val SCAN_PERIOD = 10000L
private const val TAG = "BTController"

class BluetoothController(private val _connectionFragment: ConnectionFragment) {

    private val _bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter() // TODO clean up nullables and do proper bt support checking
    private val _bluetoothLeScanner: BluetoothLeScanner? = _bluetoothAdapter.bluetoothLeScanner

    private val _btDevices: ArrayList<BluetoothDevice> = ArrayList()
    val btDevices: List<BluetoothDevice> get() = _btDevices

    private var scanning = false

    init {
        checkBluetoothSupport()
    }

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            Log.d(TAG, "BLE action type: $callbackType")
            when (callbackType) {
                ScanSettings.CALLBACK_TYPE_ALL_MATCHES -> // first match does not have a name
                    if (!_btDevices.contains(result.device)) _btDevices.add(result.device)
                ScanSettings.CALLBACK_TYPE_MATCH_LOST -> {
                    _btDevices.remove(result.device) // todo doesn't work with adapted scan settings
                    Log.d(TAG, "lost " + result.device.name)
                }
            }
            _connectionFragment.updateBtDeviceListAdapter()
        }
    }

    fun scanForDevices(hostScan: Boolean) {
        val filter = if (hostScan)
            ScanFilter.Builder()
                .setServiceUuid(ParcelUuid.fromString("00001805-0000-1000-8000-00805f9b34fb"))
                .build()
        else
            ScanFilter.Builder()
                .setServiceUuid( ParcelUuid.fromString("00001805-0000-1000-8000-00805f9b34fc"))
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
                Log.d(TAG, "scanning as host: $hostScan")
            } else {
                scanning = false
                scanner.stopScan(leScanCallback)
            }
        }
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

}