package de.htw.gezumi

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
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

    override fun onCreate(savedInstanceState : Bundle?){
        super.onCreate(savedInstanceState)

        if (btAdapter?.isEnabled == false) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
        }

        // Register for broadcasts when a device is discovered.
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context?.registerReceiver(receiver, filter)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val fragmentBinding = FragmentHostBinding.inflate(inflater, container, false)
        binding = fragmentBinding

        return fragmentBinding.root
        //return inflater.inflate(R.layout.fragment_host, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        checkPermission()
        binding?.button?.setOnClickListener {
            if (btAdapter.isDiscovering) {
                btAdapter.cancelDiscovery()
            }
            btAdapter.startDiscovery()
        }

//        binding?.apply {
//            lifecycleOwner = viewLifecycleOwner
//            viewModel = sharedViewMode
//            flavorFragment = this@FlavorFragment
//        }

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

    private val receiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("SetTextI18n")
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    // Discovery has found a device. Get the BluetoothDevice
                    // object and its info from the Intent.
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                    val name = intent.getStringExtra(BluetoothDevice.EXTRA_NAME)
                    val distance = calculateRSSI(rssi.toDouble())

                    Log.d("DLog","${binding?.rssiTextView?.text}${name}: ${distance}\n")
                    println("${binding?.rssiTextView?.text}${name}: ${distance}\n")

                    binding?.rssiTextView?.text = "${binding?.rssiTextView?.text}${name}: ${distance}\n"
                }
            }
        }
    }

    fun calculateRSSI(rssi: Double): Double {
        val txPower = -59 //hard coded power value. Usually ranges between -59 to -65
        if (rssi == 0.0) {
            return -1.0
        }
        val ratio = rssi*1.0/txPower
        return if (ratio < 1.0) {
            ratio.pow(10.0)
        } else {
            (0.89976)* ratio.pow(7.7095) + 0.111
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Don't forget to unregister the ACTION_FOUND receiver.
        context?.unregisterReceiver(receiver)
    }

}