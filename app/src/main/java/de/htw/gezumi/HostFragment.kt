package de.htw.gezumi

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
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
import de.htw.gezumi.viewmodel.GAME_ID_LENGTH
import de.htw.gezumi.viewmodel.GameViewModel

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
            //_connectedDevices.removeAt(position)
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
            Handler(Looper.getMainLooper()).post{_bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED}
            Handler(Looper.getMainLooper()).post{updateAdapters()}
        }

        override fun onGattDisconnect(device: BluetoothDevice) {
            _connectedDevices.remove(device)
            Handler(Looper.getMainLooper()).post{updateAdapters()}
        }
    }

    @kotlin.ExperimentalUnsignedTypes
    @SuppressLint("DefaultLocale")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
        val gameService = GameService.createHostService()

        _gameViewModel.gameId = GameService.GAME_ID_PREFIX + GameService.randomIdPart

        Log.d(TAG, "start gatt server and game service")
        _gattServer = GattServer(requireContext(), _gameViewModel.bluetoothController, connectCallback)
        _gattServer.startServer(gameService)
        //else bluetoothController.stopAdvertising() // TODO stop host advertise when game starts?
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_host, container, false)
        return _binding.root
    }

    @kotlin.ExperimentalUnsignedTypes
    @SuppressLint("DefaultLocale")
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

        _binding.editTextGameName.setOnEditorActionListener{ textView, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                Log.d(TAG, "game name changed")
                onGameNameChanged(textView.text.toString())
                val imm: InputMethodManager = context!!.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(_binding.editTextGameName.windowToken, 0)
                _binding.editTextGameName.clearFocus()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
    }

    @kotlin.ExperimentalUnsignedTypes
    @SuppressLint("DefaultLocale")
    private fun onGameNameChanged(gameName: String) {
        GameService.gameName = gameName // must be in game service so gattServerCallback can access it
        _gameViewModel.gameId = GameService.GAME_ID_PREFIX + GameService.randomIdPart + gameName.toByteArray(Charsets.UTF_8)

        _gameViewModel.bluetoothController.stopAdvertising()
        _gameViewModel.bluetoothController.stopScan(object: ScanCallback() {})

        _gameViewModel.bluetoothController.startAdvertising(_gameViewModel.gameId, _gameViewModel.myDeviceId)
        Log.d(TAG, "start game scan")
        _gameViewModel.bluetoothController.startScan(_gameViewModel.gameScanCallback, _gameViewModel.gameId)
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