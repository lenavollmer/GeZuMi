package de.htw.gezumi

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.bottomsheet.BottomSheetBehavior
import de.htw.gezumi.adapter.PlayerDeviceListAdapter
import de.htw.gezumi.controller.BluetoothController
import de.htw.gezumi.databinding.FragmentHostBinding
import de.htw.gezumi.gatt.GattServer

private const val TAG = "HostFragment"

class HostFragment : Fragment() {

    private lateinit var _binding: FragmentHostBinding
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>

    private val _bluetoothController: BluetoothController = BluetoothController()
    private lateinit var _gattServer: GattServer

    private val _connectedDevices: ArrayList<BluetoothDevice> = ArrayList()
    private val _playerListAdapter: PlayerDeviceListAdapter = PlayerDeviceListAdapter(_connectedDevices)

    interface GattConnectCallback {
        fun onGattConnect(device: BluetoothDevice)
        fun onGattDisconnect(device: BluetoothDevice)
    }
    // TODO refactor GattConnectCallback
    private val connectCallback = object : GattConnectCallback {
        override fun onGattConnect(device: BluetoothDevice) {
            _connectedDevices.add(device)
            Log.d(TAG, "new device connected")
            Handler(Looper.getMainLooper()).post{updatePlayerListAdapter()}
        }

        override fun onGattDisconnect(device: BluetoothDevice) {
            _connectedDevices.remove(device)
            Handler(Looper.getMainLooper()).post{updatePlayerListAdapter()}
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
        // start gatt server
        _gattServer = GattServer(requireContext(), _bluetoothController, connectCallback)
        _gattServer.startAdvertising()
        _gattServer.startServer()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_host, container, false)
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding.lifecycleOwner = viewLifecycleOwner
        _binding.recyclerPlayers.adapter = _playerListAdapter
        _binding.recyclerPlayers.apply {
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        bottomSheetBehavior = BottomSheetBehavior.from(_binding.bottomSheet.bottomSheet)
        bottomSheetBehavior.isHideable = false

//        _binding.bottomSheetDisplay.setOnClickListener {
//            val state =
//                if (bottomSheetBehavior.state == BottomSheetBehavior.STATE_EXPANDED)
//                    BottomSheetBehavior.STATE_COLLAPSED
//                else
//                    BottomSheetBehavior.STATE_EXPANDED
//            bottomSheetBehavior.state = state
//        }
    }

    // TODO handle lifecycle actions for gatt server

    override fun onDestroy() {
        super.onDestroy()
        _gattServer.stop()
    }

    override fun onPause() {
        super.onPause()
        //deviceListAdapter.clear() should we really clear all bluetooth devices here?
        // TODO maybe stop bluetooth scanning or smth
        updatePlayerListAdapter()
    }

    private fun updatePlayerListAdapter() {
        _playerListAdapter.notifyDataSetChanged()
    }
}