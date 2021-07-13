package de.htw.gezumi

import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGattService
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.app.AppCompatActivity
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
import de.htw.gezumi.util.CSVReader
import de.htw.gezumi.viewmodel.GAME_NAME_LENGTH
import de.htw.gezumi.viewmodel.GameViewModel
import kotlin.random.Random


private const val TAG = "HostFragment"

class HostFragment : Fragment() {

    private val _gameViewModel: GameViewModel by activityViewModels()
    private val _minimumPlayers = 2
    private val _maxPlayers = 3

    private lateinit var _gameService: BluetoothGattService
    private lateinit var _binding: FragmentHostBinding
    private lateinit var _bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>
    private lateinit var _gattServer: GattServer

    private var _gameStarted = false

    private val _connectedDevices: MutableList<BluetoothDevice> =
        mutableListOf() // devices that are connected, but neither approved nor declined
    private val _joinNames: MutableList<String> = mutableListOf() // depends on connectedDevices
    private lateinit var _playerListAdapter: ApprovedDevicesAdapter
    // for bottom sheet
    private val _connectedListAdapter = ConnectedPlayerDeviceAdapter(_joinNames) { position, status ->
        if (status == ConnectedPlayerDeviceAdapter.STATUS.APPROVED) {
            _gattServer.notifyJoinApproved(_connectedDevices[position], true)
            _binding.noPlayers.visibility = View.GONE
            _binding.recyclerPlayers.visibility = View.VISIBLE
            if (_gameViewModel.devices.size >= (_minimumPlayers - 1) && _gameViewModel.devices.size <= (_maxPlayers - 1)) {
                _binding.startGame.isEnabled = true
            }
        } else {
            _gattServer.notifyJoinApproved(_connectedDevices[position], false)
        }
        _connectedDevices.removeAt(position)
        _joinNames.removeAt(position)
        if (_connectedDevices.size == 0)
            _bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
        updateAdapters()
    }

    interface GattConnectCallback {
        fun onJoinRequest(bluetoothDevice: BluetoothDevice, joinName: String?)
        fun onGattDisconnect(bluetoothDevice: BluetoothDevice)
    }


    @kotlin.ExperimentalUnsignedTypes
    private val connectCallback = object : GattConnectCallback {
        override fun onJoinRequest(bluetoothDevice: BluetoothDevice, joinName: String?) {
            val joinString = joinName ?: "An unknown player"
            _connectedDevices.add(bluetoothDevice)
            _joinNames.add(joinString)
            Handler(Looper.getMainLooper()).post {
                _bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
                updateAdapters()
            }
        }

        @kotlin.ExperimentalUnsignedTypes
        override fun onGattDisconnect(bluetoothDevice: BluetoothDevice) {
            val isContained = _connectedDevices.contains(bluetoothDevice)
            if (isContained) {
                // if is contained, also remove name
                val idx = _connectedDevices.indexOf(bluetoothDevice)
                _joinNames.removeAt(idx)
                _connectedDevices.remove(bluetoothDevice) // is only present if currently neither approved nor declined
            }
            _gattServer.subscribedDevices.remove(bluetoothDevice)

            val device = GameViewModel.instance.devices.find { it.bluetoothDevice == bluetoothDevice }
            if (device != null) {
                // device was already a player of the game
                Log.d(TAG, "remove device from game: $device")
                GameViewModel.instance.devices.remove(device)
                if (_gameStarted) {
                    // could come here (HostFragment) even if game is running
                    _gattServer.notifyGameEnding()
                    _gameViewModel.onGameLeave()
                }
            }
            // update UI
            Handler(Looper.getMainLooper()).post {
                if (_gameViewModel.devices.size >= (_minimumPlayers - 1) && _gameViewModel.devices.size < (_maxPlayers - 1)) {
                    _binding.startGame.isEnabled = false
                }
                if( _gameViewModel.devices.size == 0) {
                    _binding.noPlayers.visibility = View.VISIBLE
                    _binding.recyclerPlayers.visibility = View.GONE
                }
                if (_gameViewModel.devices.size == 0) {
                    _bottomSheetBehavior.state = BottomSheetBehavior.STATE_COLLAPSED
                }
                updateAdapters()
            }
        }
    }

    @kotlin.ExperimentalUnsignedTypes
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        _playerListAdapter = ApprovedDevicesAdapter(_gameViewModel.devices)
        _gameViewModel.setPlayerListAdapter(_playerListAdapter)

        if (_gameViewModel.txPower == null)
            _gameViewModel.txPower = CSVReader.getTxPower(Build.DEVICE, requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_host, container, false)
        _playerListAdapter.lifecycleOwner = viewLifecycleOwner

        _binding.noPlayers.visibility = View.VISIBLE
        _binding.recyclerPlayers.visibility = View.GONE

        (activity as AppCompatActivity?)!!.supportActionBar?.show() // Enable Bar
        (activity as AppCompatActivity?)!!.supportActionBar?.setTitle(R.string.host)

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
            // start advertising with player name
            _gameViewModel.bluetoothController.stopAdvertising()
            _gameViewModel.bluetoothController.startAdvertising(
                _gameViewModel.gameId,
                if (_gameViewModel.playerName != null)
                    _gameViewModel.playerName!!.toByteArray(Charsets.UTF_8)
                else ByteArray(0)
            )
        }
        _binding.startGame.isEnabled = false

        val possibleGameNames = resources.getStringArray(R.array.game_names).toList()
        _binding.editTextGameName.setText(possibleGameNames[Random.nextInt(possibleGameNames.size)])
        onGameNameChanged(_binding.editTextGameName.text.toString())

        _binding.editTextGameName.setOnEditorActionListener { textView, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_GO) {
                onGameNameChanged(textView.text.toString())
                val imm: InputMethodManager =
                    requireContext().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                imm.hideSoftInputFromWindow(_binding.editTextGameName.windowToken, 0)
                _binding.editTextGameName.clearFocus()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        if (arguments?.getString("playerName") != null) {
            val playerName = arguments?.getString("playerName")!!
            _gameViewModel.onPlayerNameChanged(playerName)
        }
    }

    @kotlin.ExperimentalUnsignedTypes
    private fun onGameNameChanged(gameName: String) {
        require(gameName.length <= GAME_NAME_LENGTH) { "Game name too long" }
        GameService.gameName = gameName // must be in game service so gattServerCallback can access it
        // restart advertisement with new name
        scanAndAdvertise()
    }

    @kotlin.ExperimentalUnsignedTypes
    private fun scanAndAdvertise() {
        stopScanAndAdvertise()
        _gameViewModel.bluetoothController.startAdvertising(
            _gameViewModel.gameId,
            GameService.gameName.toByteArray(Charsets.UTF_8)
        )
        _gameViewModel.bluetoothController.startScan(_gameViewModel.gameScanCallback, _gameViewModel.gameId)
    }

    private fun stopScanAndAdvertise() {
        _gameViewModel.bluetoothController.stopAdvertising()
        _gameViewModel.bluetoothController.stopScan(GAME_SCAN_KEY)
    }

    override fun onStop() {
        super.onStop()
        if (!_gameStarted) {

            _connectedDevices.forEach { // decline all pending players
                _gattServer.notifyJoinApproved(it, false)
            }

            _connectedDevices.clear()
            _joinNames.clear()

            _binding.startGame.isEnabled = false

            _gattServer.notifyGameEnding()
            stopScanAndAdvertise()
            _gameViewModel.clearModel()
            _gattServer.stopServer()
            updateAdapters()
        }
    }

    @kotlin.ExperimentalUnsignedTypes
    override fun onStart() {
        super.onStart()
        Log.d(TAG, "start gatt server and game service")
        _gameService = GameService.createHostService()
        _gameViewModel.makeGameId()

        _gattServer = GattServer(requireContext(), _gameViewModel.bluetoothController, connectCallback)
        _gameViewModel.gattServer = _gattServer
        _gattServer.startServer(_gameService)
        scanAndAdvertise()
    }

    private fun updateAdapters() {
        _connectedListAdapter.notifyDataSetChanged()
        _playerListAdapter.notifyDataSetChanged()
    }
}
