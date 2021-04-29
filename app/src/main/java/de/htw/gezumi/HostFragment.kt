package de.htw.gezumi

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.ParcelUuid
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import de.htw.gezumi.databinding.FragmentHostBinding
import kotlin.math.pow

private const val REQUEST_ENABLE_BT = 1


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class HostFragment : Fragment() {

    private val btAdapter = BluetoothAdapter.getDefaultAdapter()
    private var binding: FragmentHostBinding? = null
    private val bluetoothLeScanner: BluetoothLeScanner? = btAdapter.bluetoothLeScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (btAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        // Inflate the layout for this fragment
        val fragmentBinding = FragmentHostBinding.inflate(inflater, container, false)
        binding = fragmentBinding

        return fragmentBinding.root
        //return inflater.inflate(R.layout.fragment_host, container, false)
    }

    // Stops scanning after 10 seconds.
    private val SCAN_PERIOD: Long = 10000
    private var scanning = false
    private val handler = Handler()

    private fun scanLeDevice() {
        val filter = ScanFilter.Builder().setServiceUuid(
            ParcelUuid.fromString("00001805-0000-1000-8000-00805f9b34fb"),
        ).build()
        val scanSettings = ScanSettings.Builder().build()

        bluetoothLeScanner?.let { scanner ->
            if (!scanning) { // Stops scanning after a pre-defined scan period.
                handler.postDelayed({
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

    // Device scan callback.
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            val distance = calculateRSSI(result.rssi.toDouble())
            binding?.rssiTextView?.text = "${binding?.rssiTextView?.text}${result.device.name}: ${distance}\n"
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkPermission()
        binding?.button?.setOnClickListener {
//            if (btAdapter.isDiscovering) {
//                btAdapter.cancelDiscovery()
//            }
//            btAdapter.startDiscovery()
            scanLeDevice()
        }
    }

    private fun checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (!(context?.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && context?.checkSelfPermission(
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED)
            ) {
                ActivityCompat.requestPermissions(
                    context as Activity, arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ), 1
                )
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        if (!(requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
            checkPermission()
        }
    }

    fun calculateRSSI(rssi: Double): Double {
        val txPower = -59 //hard coded power value. Usually ranges between -59 to -65
        if (rssi == 0.0) {
            return -1.0
        }
        val ratio = rssi * 1.0 / txPower
        return if (ratio < 1.0) {
            ratio.pow(10.0)
        } else {
            (0.89976) * ratio.pow(7.7095) + 0.111
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Don't forget to unregister the ACTION_FOUND receiver.
    }

}