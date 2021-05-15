package de.htw.gezumi

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.DividerItemDecoration
import de.htw.gezumi.adapter.HostDeviceListAdapter
import de.htw.gezumi.databinding.FragmentClientBinding
import de.htw.gezumi.gatt.GameService
import de.htw.gezumi.viewmodel.GameViewModel

private const val TAG = "ClientFragment"

class ClientFragment : Fragment() {

    private val _gameViewModel: GameViewModel by activityViewModels()

    private lateinit var _binding: FragmentClientBinding

    private val _availableHostDevices: ArrayList<BluetoothDevice> = ArrayList()
    private val _hostDeviceListAdapter: HostDeviceListAdapter = HostDeviceListAdapter(_availableHostDevices)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _gameViewModel.hostScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                Log.d(TAG, "host scan callback")
                when (callbackType) {
                    ScanSettings.CALLBACK_TYPE_ALL_MATCHES -> // first match does not have a name
                        if (!_availableHostDevices.contains(result.device)) _availableHostDevices.add(result.device)
                    ScanSettings.CALLBACK_TYPE_MATCH_LOST -> {
                        _availableHostDevices.remove(result.device) // todo doesn't work with adapted scan settings
                        Log.d(TAG, "lost " + result.device.name)
                    }
                }
                updateBtDeviceListAdapter()
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_client, container, false)
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding.lifecycleOwner = viewLifecycleOwner
        _binding.recyclerBtDevices.adapter = _hostDeviceListAdapter
        _binding.recyclerBtDevices.apply {
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
        _binding.button.setOnClickListener {
            _gameViewModel.bluetoothController.startScan(_gameViewModel.hostScanCallback, ParcelUuid(GameService.HOST_UUID))
        }

        checkPermission()
    }

    override fun onPause() {
        super.onPause()
        // TODO maybe stop bluetooth scanning or smth
        updateBtDeviceListAdapter()
    }

    private fun updateBtDeviceListAdapter() {
        _hostDeviceListAdapter.notifyDataSetChanged()
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