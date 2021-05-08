package de.htw.gezumi

import android.bluetooth.*
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import de.htw.gezumi.databinding.FragmentGameBinding
import de.htw.gezumi.gatt.GattClient
import de.htw.gezumi.gatt.GattClientCallback
import de.htw.gezumi.model.Device
import de.htw.gezumi.viewmodel.DevicesViewModel
import java.util.*

private const val TAG = "GameFragment"

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class GameFragment : Fragment() {

    private lateinit var _binding: FragmentGameBinding
    private val _devicesViewModel: DevicesViewModel by viewModels()

    private lateinit var _gattClient: GattClient
    private lateinit var _hostDevice: BluetoothDevice

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _hostDevice = arguments?.getParcelable("hostDevice")!!

        val hostDevice = Device(_hostDevice.address, -70)
        hostDevice.setName(_hostDevice.name)
        _devicesViewModel.host = hostDevice
        Log.d(TAG, "host: ${hostDevice.name} address: ${hostDevice.address}")
        _devicesViewModel.addDevice(_devicesViewModel.host)

        val gattClientCallback = GattClientCallback(_devicesViewModel)
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
        _binding.devicesViewModel = _devicesViewModel
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