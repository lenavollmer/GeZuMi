package de.htw.gezumi

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGatt
import android.bluetooth.le.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import de.htw.gezumi.databinding.FragmentHostBinding
import de.htw.gezumi.util.BtDeviceListAdapter
import kotlin.math.pow


private const val REQUEST_ENABLE_BT = 1
private const val SCAN_PERIOD = 10000L


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class HostFragment : Fragment() {

    private val btAdapter = BluetoothAdapter.getDefaultAdapter()
    private var binding: FragmentHostBinding? = null
    private val bluetoothLeScanner: BluetoothLeScanner? = btAdapter.bluetoothLeScanner
    // Stops scanning after 10 seconds.
    private var scanning = false
    private val deviceListAdapter: BtDeviceListAdapter = BtDeviceListAdapter()


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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding?.btDevices?.adapter = deviceListAdapter
        binding?.btDevices?.layoutManager = LinearLayoutManager(activity)
        checkPermission()
        binding?.button?.setOnClickListener {
            scanforDevice()
        }
    }

    override fun onPause() {
        super.onPause()
        deviceListAdapter.clear()
    }

    private fun scanforDevice() {
        val filter = ScanFilter.Builder().setServiceUuid(
            ParcelUuid.fromString("00001805-0000-1000-8000-00805f9b34fb"),
        ).build()
        val scanSettings = ScanSettings.Builder().build()

        bluetoothLeScanner?.let { scanner ->
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

    // Device scan callback.
    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            deviceListAdapter.addDevice(result.device)
            deviceListAdapter.notifyDataSetChanged()
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



    override fun onDestroy() {
        super.onDestroy()
        // Don't forget to unregister the ACTION_FOUND receiver.
    }

}