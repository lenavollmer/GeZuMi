package de.htw.gezumi.controller

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.*
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import kotlin.collections.ArrayList

private const val SCAN_PERIOD = 10000L
private const val TAG = "BTController"

class BluetoothController() {

    private val _bluetoothAdapter: BluetoothAdapter = BluetoothAdapter.getDefaultAdapter() // TODO clean up nullables and do proper bt support checking
    private val _bluetoothLeScanner: BluetoothLeScanner? = _bluetoothAdapter.bluetoothLeScanner

    private var scanning = false

    init {
        checkBluetoothSupport()
    }

    fun scanForDevices(leScanCallback: ScanCallback) {
        val filter = ScanFilter.Builder()
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
                Log.d(TAG, "scanning as client")
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

}