package de.htw.gezumi

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.*
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import java.nio.channels.ScatteringByteChannel
import java.util.*
import kotlin.collections.ArrayList

private const val SCAN_PERIOD = 10000L

class BluetoothController(private val _hostFragment: HostFragment) {

    private val TAG = "BTController"

    private val _btAdapter = BluetoothAdapter.getDefaultAdapter()
    private val _bluetoothLeScanner: BluetoothLeScanner? = _btAdapter.bluetoothLeScanner

    private val _btDevices: ArrayList<BluetoothDevice> = ArrayList()
    val btDevices: List<BluetoothDevice> get() = _btDevices

    private var scanning = false

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
            _hostFragment.updateBtDeviceListAdapter()
        }
    }

    fun scanForDevices() {
        val filter = ScanFilter.Builder().setServiceUuid(
            ParcelUuid.fromString("00001805-0000-1000-8000-00805f9b34fb")
        ).build()
        val scanSettings = ScanSettings.Builder().setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES).build()

        _bluetoothLeScanner?.let { scanner ->
            if (!scanning) { // Stops scanning after a pre-defined scan period.
                Handler(Looper.getMainLooper()).postDelayed({
                    scanning = false
                    scanner.stopScan(leScanCallback)
                }, SCAN_PERIOD)
                scanning = true
                scanner.startScan(listOf(filter), scanSettings, leScanCallback)
            } else {
                scanning = false
                scanner.stopScan(leScanCallback)
            }
        }
    }


}