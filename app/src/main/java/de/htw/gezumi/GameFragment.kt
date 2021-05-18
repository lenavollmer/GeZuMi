package de.htw.gezumi

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
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

    private val _gameViewModel: GameViewModel by activityViewModels()

    private lateinit var _binding: FragmentGameBinding

    // bluetooth stuff also in game fragment or is it possible to manage all that in client and host?
    private lateinit var _gattClient: GattClient
    private lateinit var _hostDevice: BluetoothDevice

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _hostDevice = arguments?.getParcelable("hostDevice")!!

        val hostDevice = Device(_hostDevice.address, -70, _hostDevice)
        //hostDevice.setName(_hostDevice.name)
        _gameViewModel.host = hostDevice
        Log.d(TAG, "host: ${hostDevice.name} address: ${hostDevice.address}")
        _gameViewModel.addDevice(hostDevice)

        val gattClientCallback = GattClientCallback(_gameViewModel)
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
        _gameViewModel.writeRSSILog()
        _gattClient.disconnect()
    }

    override fun onResume() {
        super.onResume()
        _gattClient.reconnect()
    }

    override fun onStop() {
        super.onStop()
        // stop scan and advertise
        // TODO on resume has to start it again (but not twice!) -> implement pause/disconnect functionality
        _gameViewModel.onGameLeave()
    }
}