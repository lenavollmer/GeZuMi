package de.htw.gezumi

import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import de.htw.gezumi.adapter.JoinGameListAdapter
import de.htw.gezumi.callbacks.GameJoinUICallback
import de.htw.gezumi.callbacks.GameLeaveUICallback
import de.htw.gezumi.controller.HOST_SCAN_KEY
import de.htw.gezumi.databinding.FragmentClientBinding
import de.htw.gezumi.databinding.PopupJoinBinding
import de.htw.gezumi.gatt.GameService
import de.htw.gezumi.gatt.GattClient
import de.htw.gezumi.gatt.GattClientCallback
import de.htw.gezumi.model.Device
import de.htw.gezumi.util.CSVReader
import de.htw.gezumi.util.dimBehind
import de.htw.gezumi.viewmodel.GameViewModel

private const val TAG = "ClientFragment"

class ClientFragment : Fragment() {

    private val _gameViewModel: GameViewModel by activityViewModels()

    private lateinit var _binding: FragmentClientBinding
    private lateinit var _popupBinding: PopupJoinBinding
    private lateinit var _popupWindow: PopupWindow
    private lateinit var _gattClient: GattClient

    private var _gameStarted = false
    private var _connected = false

    private var _playerName = "gustav"

    private val _availableHostDevices: ArrayList<Device> = ArrayList()

    @kotlin.ExperimentalUnsignedTypes
    private val _hostDeviceListAdapter: JoinGameListAdapter = JoinGameListAdapter(_availableHostDevices) {

        try {
            _gameViewModel.host = _availableHostDevices[it]
            _gameViewModel.game.hostId = _availableHostDevices[it].deviceId

            val gattClientCallback = GattClientCallback()
            if (_gattClient.connect(_availableHostDevices[it].bluetoothDevice!!, gattClientCallback) == true) {
                _connected = true
                _gameViewModel.gameJoinUICallback = gameJoinUICallback
                _gameViewModel.gameLeaveUICallback = gameLeaveUICallback
                _gameViewModel.gattClient = _gattClient

                _popupBinding.joinText.text = getString(R.string.join_wait)
                _popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)
                _popupWindow.dimBehind()


                _gameViewModel.bluetoothController.stopScan(HOST_SCAN_KEY)
                _binding.buttonScan.isEnabled = false
                _availableHostDevices.clear()
                updateBtDeviceListAdapter()
            } else {
                _availableHostDevices.clear()
                updateBtDeviceListAdapter()
                Log.d(TAG, "Data was not send correctly")
            }
        } catch (e: IndexOutOfBoundsException) {
            _availableHostDevices.clear()
            updateBtDeviceListAdapter()
            Log.d(TAG, "Game did not exist")
        }
    }

    @kotlin.ExperimentalUnsignedTypes
    private val gameLeaveUICallback = object : GameLeaveUICallback {
        override fun gameLeft() {
            Handler(Looper.getMainLooper()).post {
                Log.d(TAG, "game ended by host")
                Toast.makeText(context, R.string.game_closed, Toast.LENGTH_LONG).show()
                _binding.buttonScan.isEnabled = true
                resetConnection()
            }
        }
    }

    @kotlin.ExperimentalUnsignedTypes
    private val gameJoinUICallback = object : GameJoinUICallback {
        override fun gameJoined() {
            Handler(Looper.getMainLooper()).post {
                _popupWindow.dismiss()
                _popupBinding.joinText.text = getString(R.string.join_approved)
                _popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)
                _popupWindow.dimBehind()
            }
        }

        override fun gameDeclined() {
            Handler(Looper.getMainLooper()).post {
                Toast.makeText(context, R.string.join_declined, Toast.LENGTH_LONG).show()
                _connected = false
                resetConnection()
            }

        }

        override fun gameStarted() {
            Handler(Looper.getMainLooper()).post {
                _gameStarted = true
                _popupWindow.dismiss()
                Log.d(TAG, "game started")
                findNavController().navigate(R.id.action_ClientFragment_to_Game)
            }
        }
    }

    @kotlin.ExperimentalUnsignedTypes
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _gameViewModel.gameId = GameService.GAME_ID_PREFIX

        if (_gameViewModel.txPower == null)
            _gameViewModel.txPower = CSVReader.getTxPower(Build.DEVICE, requireContext())

        _gameViewModel.hostScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                val deviceId = GameService.extractDeviceId(result)
                val txPower = GameService.extractTxPower(result)
                when (callbackType) {
                    ScanSettings.CALLBACK_TYPE_ALL_MATCHES -> {
                        if (!Utils.contains(_availableHostDevices, deviceId)) {
                            _availableHostDevices.add(Device(deviceId, txPower, result.device))
                            return
                        }

                        val device = Utils.findDevice(_availableHostDevices, deviceId)!!
                        // check for new game name
                        val newGameName = GameService.extractName(result)
                        if (!device.gameName.equals(newGameName))
                            device.gameName.postValue(newGameName)
                        // refresh bt device: if game name changed, host uses a new bt device
                        if (device.bluetoothDevice != result.device)
                            device.bluetoothDevice = result.device
                    }
                }
                updateBtDeviceListAdapter()
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                Log.d(TAG, "scan failed: $errorCode")
            }
        }
    }

    @kotlin.ExperimentalUnsignedTypes
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_client, container, false)
        _popupBinding = DataBindingUtil.inflate(inflater, R.layout.popup_join, null, false)

        _hostDeviceListAdapter.lifecycleOwner = viewLifecycleOwner

        (activity as AppCompatActivity?)!!.supportActionBar?.show() // Enable Bar
        (activity as AppCompatActivity?)!!.supportActionBar?.setTitle(R.string.join)

        return _binding.root
    }

    @kotlin.ExperimentalUnsignedTypes
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding.lifecycleOwner = viewLifecycleOwner
        _binding.recyclerBtDevices.adapter = _hostDeviceListAdapter
        _binding.recyclerBtDevices.apply {
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
        _binding.buttonScan.setOnClickListener {
            resetConnection()
            _gameViewModel.onPlayerNameChanged(_playerName)
            _gameViewModel.bluetoothController.startHostScan(_gameViewModel.hostScanCallback)
        }

        if (arguments?.getString("playerName") != null) {
            _playerName = arguments?.getString("playerName")!!
        }

        _popupWindow = PopupWindow(
            _popupBinding.root,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )
        _popupWindow.isOutsideTouchable = false
        _popupWindow.isFocusable = false


        _gattClient = GattClient(requireContext())
    }

    @kotlin.ExperimentalUnsignedTypes
    private fun resetConnection() {
        _popupWindow.dismiss()
        _availableHostDevices.clear()
        updateBtDeviceListAdapter()

        if (_connected) _gattClient.disconnect()

        _gameViewModel.bluetoothController.stopScan(HOST_SCAN_KEY)
        _gameViewModel.bluetoothController.stopAdvertising()
        _gameViewModel.clearGameId()

        _binding.buttonScan.isEnabled = true
    }

    @kotlin.ExperimentalUnsignedTypes
    override fun onStop() {
        super.onStop()
        _popupWindow.dismiss()
        if (!_gameStarted) {
            resetConnection()
        }
    }

    @kotlin.ExperimentalUnsignedTypes
    private fun updateBtDeviceListAdapter() {
        _hostDeviceListAdapter.notifyDataSetChanged()
    }

}