package de.htw.gezumi

import android.bluetooth.BluetoothDevice
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
import de.htw.gezumi.controller.GAME_SCAN_KEY
import de.htw.gezumi.databinding.FragmentHostBinding
import de.htw.gezumi.gatt.GameService
import de.htw.gezumi.gatt.GattServer
import de.htw.gezumi.viewmodel.GameViewModel

private const val TAG = "HostFragment"

class HostFragment : Fragment() {

    private val _gameViewModel: GameViewModel by activityViewModels()

    private lateinit var _binding: FragmentHostBinding
    private lateinit var _bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>

    private lateinit var _gattServer: GattServer

    private var _gameStarted = false

    private val _connectedDevices: ArrayList<BluetoothDevice> = ArrayList() // devices that are connected, but neither approved nor declined
    //private val _approvedDevices: ArrayList<Device> = ArrayList()
    // for displayed list
    private lateinit var _playerListAdapter: ApprovedDevicesAdapter
    // for bottom sheet
    private val _connectedListAdapter = ConnectedPlayerDeviceAdapter(_connectedDevices) { position, status ->
        if (status == ConnectedPlayerDeviceAdapter.STATUS.APPROVED) {
            //_approvedDevices.add(_connectedDevices[position])
            _gattServer.notifyJoinApproved(_connectedDevices[position], true)
        }
        else {
            _gattServer.notifyJoinApproved(_connectedDevices[position], false)
        }
        _connectedDevices.removeAt(position)
        if (_connectedDevices.size == 0)
            _bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        updateAdapters()
    }

    interface GattConnectCallback {
        fun onGattConnect(bluetoothDevice: BluetoothDevice)
        fun onGattDisconnect(bluetoothDevice: BluetoothDevice)
    }
    // TODO refactor GattConnectCallback
    private val connectCallback = object : GattConnectCallback {
        override fun onGattConnect(bluetoothDevice: BluetoothDevice) {
            _connectedDevices.add(bluetoothDevice)
            Handler(Looper.getMainLooper()).post{
                _bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                updateAdapters()
            }
        }

        override fun onGattDisconnect(bluetoothDevice: BluetoothDevice) {
            _connectedDevices.remove(bluetoothDevice) // is only present if currently neither approved nor declined
            _gattServer.subscribedDevices.remove(bluetoothDevice)

            // remove device TODO: remove player
            val device = GameViewModel.instance.devices.find{it.bluetoothDevice == bluetoothDevice}
            Log.d(TAG, "REMOVEEEEEEEEEEEEEEE DEVICE: $device")
            GameViewModel.instance.devices.remove(device)
            // update UI
            Handler(Looper.getMainLooper()).post{
                if (_connectedDevices.size == 0)
                    _bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                updateAdapters()
            }
        }
    }

    @kotlin.ExperimentalUnsignedTypes
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }

        _playerListAdapter = ApprovedDevicesAdapter(_gameViewModel.devices)
        _gameViewModel.setPlayerListAdapter(_playerListAdapter)


        val gameService = GameService.createHostService()

        _gameViewModel.gameId = GameService.HOST_ID_PREFIX + GameService.randomIdPart

        Log.d(TAG, "start gatt server and game service")
        _gattServer = GattServer(requireContext(), _gameViewModel.bluetoothController, connectCallback)
        _gameViewModel.gattServer = _gattServer
        _gattServer.startServer(gameService)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_host, container, false)
        _playerListAdapter.lifecycleOwner = viewLifecycleOwner
        return _binding.root
    }

    @kotlin.ExperimentalUnsignedTypes
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
            _gameStarted = true
            findNavController().navigate(R.id.action_HostFragment_to_Game)
        }

        _binding.editTextGameName.setText(R.string.default_game_name)
        onGameNameChanged(_binding.editTextGameName.text.toString())

        _binding.editTextGameName.setOnEditorActionListener{ textView, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                Log.d(TAG, "game name changed")
                onGameNameChanged(textView.text.toString())
                val imm: InputMethodManager = requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(_binding.editTextGameName.windowToken, 0)
                _binding.editTextGameName.clearFocus()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

    }

    @kotlin.ExperimentalUnsignedTypes
    private fun onGameNameChanged(gameName: String) {
        require(gameName.length <= 8) {"Game name too long"}
        GameService.gameName = gameName // must be in game service so gattServerCallback can access it
        // restart advertisement with new name
        scanAndAdvertise()
    }

    @kotlin.ExperimentalUnsignedTypes
    private fun scanAndAdvertise() {
        stopScanAndAdvertise()
        _gameViewModel.bluetoothController.startAdvertising(_gameViewModel.gameId, GameService.gameName.toByteArray(Charsets.UTF_8))
        _gameViewModel.bluetoothController.startScan(_gameViewModel.gameScanCallback, _gameViewModel.gameId)
    }

    private fun stopScanAndAdvertise() {
        _gameViewModel.bluetoothController.stopAdvertising()
        _gameViewModel.bluetoothController.stopScan(GAME_SCAN_KEY)
    }

    override fun onDestroy() {
        super.onDestroy()
        _gameViewModel.clearModel()
        _gattServer.stopServer()
    }

    override fun onPause() {
        super.onPause()
        wasOnPause = true
        updateAdapters()
        if(!_gameStarted) stopScanAndAdvertise()
        //_gameViewModel.bluetoothController.stopAdvertising()
        //_gameViewModel.bluetoothController.stopScan(GAME_SCAN_KEY) // why just stop scan?? TODO: klären!
    }
    var wasOnPause = false
    @kotlin.ExperimentalUnsignedTypes
    override fun onResume() {
        super.onResume()
        if (wasOnPause) {
            wasOnPause = false
            scanAndAdvertise()
            // _gameViewModel.bluetoothController.startAdvertising(_gameViewModel.gameId, GameService.gameName.toByteArray(Charsets.UTF_8)) why just restart advertisement??
        }
    }

    private fun updateAdapters() {
        _connectedListAdapter.notifyDataSetChanged()
        _playerListAdapter.notifyDataSetChanged()
    }
}
