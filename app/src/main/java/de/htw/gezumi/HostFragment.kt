package de.htw.gezumi

import android.bluetooth.BluetoothDevice
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.bottomsheet.BottomSheetBehavior
import de.htw.gezumi.adapter.ApprovedDevicesAdapter
import de.htw.gezumi.adapter.ConnectedPlayerDeviceAdapter
import de.htw.gezumi.databinding.FragmentHostBinding
import de.htw.gezumi.gatt.GameService
import de.htw.gezumi.gatt.GattServer
import de.htw.gezumi.viewmodel.GameViewModel
import kotlin.collections.ArrayList
import de.htw.gezumi.model.Device

private const val TAG = "HostFragment"

class HostFragment : Fragment() {

    private val _gameViewModel: GameViewModel by activityViewModels()

    private lateinit var _binding: FragmentHostBinding
    private lateinit var _bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>

    private lateinit var _gattServer: GattServer

    private val _connectedDevices: ArrayList<BluetoothDevice> = ArrayList() // devices that are connected, but neither approved nor declined
    private val _approvedDevices: ArrayList<BluetoothDevice> = ArrayList()
    private val _playerListAdapter: ApprovedDevicesAdapter = ApprovedDevicesAdapter(_approvedDevices)
    private val _connectedListAdapter = ConnectedPlayerDeviceAdapter(_connectedDevices) { position, status ->
        if (status == ConnectedPlayerDeviceAdapter.STATUS.APPROVED) {
            _approvedDevices.add(_connectedDevices[position])
            _gattServer.notifyJoinApproved(_connectedDevices[position], true)
            _connectedDevices.removeAt(position)
        }
        else {
            _gattServer.notifyJoinApproved(_connectedDevices[position], false)
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
            Handler(Looper.getMainLooper()).post{
                _bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                updateAdapters()
            }
        }

        override fun onGattDisconnect(device: BluetoothDevice) {
            _connectedDevices.remove(device)
            _approvedDevices.remove(device)
            Handler(Looper.getMainLooper()).post{updateAdapters()}
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
        _gameViewModel.gameId = GameService.getGameId()

        Log.d(TAG, "start gatt server and game service")
        _gattServer = GattServer(requireContext(), _gameViewModel.bluetoothController, connectCallback)
        _gattServer.startServer(GameService.createHostService())
        _gameViewModel.bluetoothController.startAdvertising(ParcelUuid(_gameViewModel.gameId))
        Log.d(TAG, "start game scan: ${_gameViewModel.gameId}")
        _gameViewModel.bluetoothController.startScan(_gameViewModel.gameScanCallback, ParcelUuid(_gameViewModel.gameId))
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

        _binding.startGame.setOnClickListener {
            _gattServer.notifyGameStart()
            findNavController().navigate(R.id.action_HostFragment_to_Game)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        _gattServer.stopServer()
    }

    override fun onPause() {
        super.onPause()
        updateAdapters()
        _gattServer.pauseServer()
    }

    override fun onResume() {
        super.onResume()
        _gameViewModel.bluetoothController.startAdvertising(ParcelUuid(_gameViewModel.gameId))
    }

    private fun updateAdapters() {
        _connectedListAdapter.notifyDataSetChanged()
        _playerListAdapter.notifyDataSetChanged()
    }
}