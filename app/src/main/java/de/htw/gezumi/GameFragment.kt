package de.htw.gezumi

import android.bluetooth.*
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import de.htw.gezumi.databinding.FragmentGameBinding
import de.htw.gezumi.model.DeviceViewModel
import java.util.*

private const val TAG = "GameFragment"

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class GameFragment : Fragment() {

    private lateinit var _binding: FragmentGameBinding
    private val _deviceViewModel: DeviceViewModel by viewModels()

    private lateinit var _gatt: BluetoothGatt
    private lateinit var _gattClientCallback: GattClientCallback
    private lateinit var _currentDevice: BluetoothDevice
    private var _rssiTimer = Timer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _currentDevice = arguments?.getParcelable("device")!!
        _gattClientCallback = GattClientCallback(_deviceViewModel)
        _gatt = _currentDevice.connectGatt(activity, false, _gattClientCallback)
        var success1 = _gatt.connect()
        Log.d(TAG, "connected to gatt: $success1")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_game, container, false)
        _deviceViewModel.setName(_currentDevice.name)
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding.lifecycleOwner = viewLifecycleOwner
        _binding.deviceViewModel = _deviceViewModel
    }

    override fun onPause() {
        super.onPause()
        _gatt.disconnect()
    }

    override fun onResume() {
        super.onResume()
        _gatt.connect()
    }

}