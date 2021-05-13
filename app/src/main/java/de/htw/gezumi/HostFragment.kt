package de.htw.gezumi

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.bottomsheet.BottomSheetBehavior
import de.htw.gezumi.adapter.ConnectedPlayerDeviceAdapter
import de.htw.gezumi.adapter.PlayerDeviceListAdapter
import de.htw.gezumi.controller.BluetoothController
import de.htw.gezumi.databinding.FragmentHostBinding
import de.htw.gezumi.gatt.GattServer

private const val TAG = "HostFragment"

class HostFragment : Fragment() {

    private lateinit var _binding: FragmentHostBinding
    private lateinit var _bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>

    private val _bluetoothController: BluetoothController = BluetoothController()
    private lateinit var _gattServer: GattServer

    private val _connectedDevices: ArrayList<BluetoothDevice> = ArrayList() // device that are connected, but neither approved nor declined
    private val _approvedDevices: ArrayList<BluetoothDevice> = ArrayList()
    private val _playerListAdapter: PlayerDeviceListAdapter = PlayerDeviceListAdapter(_approvedDevices)
    private val _connectedListAdapter: ConnectedPlayerDeviceAdapter = ConnectedPlayerDeviceAdapter(_connectedDevices) { position, status ->
        if (status == ConnectedPlayerDeviceAdapter.STATUS.APPROVED) {
            _approvedDevices.add(_connectedDevices[position])
            _gattServer.
            _connectedDevices.removeAt(position)
        }
        else {
            // Todo add code to let device know that they are rejected
            _connectedDevices.removeAt(position)
        }
        updateAdapters()
    }

    interface GattConnectCallback {
        fun onGattConnect(device: BluetoothDevice)
        fun onGattDisconnect(device: BluetoothDevice)
    }
    // TODO refactor GattConnectCallback
    private val connectCallback = object : GattConnectCallback {
        override fun onGattConnect(device: BluetoothDevice) {
            _connectedDevices.add(device)
            _bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
            Handler(Looper.getMainLooper()).post{updateAdapters()}
        }

        override fun onGattDisconnect(device: BluetoothDevice) {
            _connectedDevices.remove(device)
            Handler(Looper.getMainLooper()).post{updateAdapters()}
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _bluetoothController.setContext(requireContext())
        arguments?.let {

        }
        // start gatt server
        _gattServer = GattServer(requireContext(), _bluetoothController, connectCallback)
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
        _binding.bottomSheet.playersToJoin.adapter = _connectedListAdapter
        _binding.bottomSheet.playersToJoin.apply {
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }

        _bottomSheetBehavior = BottomSheetBehavior.from(_binding.bottomSheet.bottomSheet)

        _bottomSheetBehavior.isHideable = false
        _bottomSheetBehavior.state = BottomSheetBehavior.STATE_HIDDEN
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
        updateAdapters()
    }

    private fun updateAdapters() {
        _connectedListAdapter.notifyDataSetChanged()
        _playerListAdapter.notifyDataSetChanged()
    }
}