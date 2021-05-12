package de.htw.gezumi

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import de.htw.gezumi.controller.BluetoothController
import de.htw.gezumi.databinding.FragmentGameBinding
import de.htw.gezumi.gatt.GattClient
import de.htw.gezumi.gatt.GattClientCallback
import de.htw.gezumi.model.Device
import de.htw.gezumi.viewmodel.GameViewModel

private const val TAG = "GameFragment"

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class GameFragment : Fragment() {

    private lateinit var _binding: FragmentGameBinding
    private val _gameViewModel: GameViewModel by viewModels()

    // bluetooth stuff also in game fragment or is it possible to manage all that in client and host
    private val _bluetoothController: BluetoothController = BluetoothController(requireContext())
    private lateinit var _gattClient: GattClient
    private lateinit var _hostDevice: BluetoothDevice

    interface GameJoinCallback {
        fun onGameJoin()
        fun onGameLeave()
    }
    // TODO refactor GattConnectCallback
    private val gameJoinCallback = object : GameJoinCallback {
        override fun onGameJoin() {
            Log.d(TAG, "on game join")
            Handler(Looper.getMainLooper()).post{}
        }

        override fun onGameLeave() {
            Log.d(TAG, "on game leave")
            Handler(Looper.getMainLooper()).post{}
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _hostDevice = arguments?.getParcelable("hostDevice")!!

        val hostDevice = Device(_hostDevice.address, -70)
        hostDevice.setName(_hostDevice.name)
        _gameViewModel.host = hostDevice
        Log.d(TAG, "host: ${hostDevice.name} address: ${hostDevice.address}")
        _gameViewModel.addDevice(_gameViewModel.host)

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
        _binding.devicesViewModel = _gameViewModel
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