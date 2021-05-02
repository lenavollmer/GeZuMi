package de.htw.gezumi

import android.Manifest
import android.app.Activity
import android.bluetooth.le.*
import android.content.pm.PackageManager
import android.os.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import de.htw.gezumi.adapter.BtDeviceListAdapter
import de.htw.gezumi.databinding.FragmentConnectionBinding


/**
 * A simple [Fragment] subclass as the second destination in the navigation.
 */
class ConnectionFragment : Fragment() {

    private lateinit var binding: FragmentConnectionBinding

    private val bluetoothController: BluetoothController = BluetoothController(this)
    private val deviceListAdapter: BtDeviceListAdapter = BtDeviceListAdapter(bluetoothController.btDevices)
    private lateinit var gattServer: GattServer

    private var _isHost: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _isHost = arguments?.getBoolean("isHost")!!
        gattServer = GattServer(requireContext(), _isHost, bluetoothController)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_connection, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.recyclerBtDevices.adapter = deviceListAdapter
        binding.recyclerBtDevices.apply {
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
        binding.button.setOnClickListener {
            bluetoothController.scanForDevices(_isHost)
        }

        checkPermission()
    }

    override fun onStart() {
        super.onStart()
        //gattServer.startViewModel()
    }

    override fun onStop() {
        super.onStop()
        //gattServer.stopViewModel()
    }

    override fun onDestroy() {
        super.onDestroy()
        //gattServer.destroyViewModel()
    }

    override fun onPause() {
        super.onPause()
        //deviceListAdapter.clear() should we really clear all bluetooth devices here?
        // TODO maybe stop bluetooth scanning or smth
        updateBtDeviceListAdapter();
    }

    fun updateBtDeviceListAdapter() {
        deviceListAdapter.notifyDataSetChanged()
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

}