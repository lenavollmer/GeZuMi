package de.htw.gezumi

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import de.htw.gezumi.controller.BluetoothController
import de.htw.gezumi.databinding.FragmentGameBinding
import de.htw.gezumi.gatt.GattClient
import de.htw.gezumi.gatt.GattClientCallback
import de.htw.gezumi.model.Device
import de.htw.gezumi.viewmodel.GameViewModel
import java.util.*

private const val TAG = "GameFragment"

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class GameFragment : Fragment() {

    private lateinit var _binding: FragmentGameBinding
    private val _gameViewModel: GameViewModel by viewModels()

    // bluetooth stuff also in game fragment or is it possible to manage all that in client and host?
    private val _bluetoothController: BluetoothController = BluetoothController()
    private lateinit var _gattClient: GattClient
    private lateinit var _hostDevice: BluetoothDevice

    private val gameScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            Log.d(TAG, "BLE action type: $callbackType")
            when (callbackType) {
                ScanSettings.CALLBACK_TYPE_ALL_MATCHES -> {
                    _gameViewModel.onGameScanResult(result.device, result.rssi)
                }
                ScanSettings.CALLBACK_TYPE_MATCH_LOST -> {
                    Log.d(TAG, "lost " + result.device.name)
                    // when do we delete a device?
                }
            }
        }
    }

    interface GameJoinCallback {
        fun onGameJoin()
        fun onGameLeave()
    }

    private val gameJoinCallback = object : GameJoinCallback {
        override fun onGameJoin() {
            Log.d(TAG, "on game join")
            Handler(Looper.getMainLooper()).post{Toast.makeText(requireContext(), "Joined", Toast.LENGTH_LONG).show()}
            val gameUuid = ParcelUuid.fromString(_gameViewModel.gameId)
            // waiting for game start is not necessary
            Log.d(TAG, "start advertising on game id")
            _bluetoothController.startAdvertising(gameUuid)
            Log.d(TAG, "start scanning for players on game id")
            _bluetoothController.scanForDevices(gameScanCallback, gameUuid)
        }

        override fun onGameLeave() {
            Log.d(TAG, "on game leave")
            // TODO
            Handler(Looper.getMainLooper()).post{}
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _hostDevice = arguments?.getParcelable("hostDevice")!!

        _bluetoothController.setContext(requireContext())

        val hostDevice = Device(_hostDevice.address, -70, _hostDevice)
        hostDevice.setName(_hostDevice.name)
        _gameViewModel.host = hostDevice
        Log.d(TAG, "host: ${hostDevice.name} address: ${hostDevice.address}")
        _gameViewModel.addDevice(hostDevice)

        val gattClientCallback = GattClientCallback(_gameViewModel, gameJoinCallback)
        _gattClient = GattClient(requireContext())

        // connect
        _gattClient.connect(_hostDevice, gattClientCallback)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_game, container, false)
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding.lifecycleOwner = viewLifecycleOwner
        _binding.gameViewModel = _gameViewModel
    }

    override fun onPause() {
        super.onPause()
        _gattClient.disconnect()
    }

    override fun onResume() {
        super.onResume()
        _gattClient.reconnect()
    }
}