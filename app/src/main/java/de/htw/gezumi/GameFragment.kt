package de.htw.gezumi

import android.graphics.Point
import android.graphics.Paint
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import de.htw.gezumi.databinding.FragmentGameBinding
import de.htw.gezumi.gatt.GattClient
import de.htw.gezumi.gatt.GattClientCallback
import de.htw.gezumi.viewmodel.DevicesViewModel
import android.util.Log
import android.graphics.Canvas
import android.graphics.Color
import android.util.DisplayMetrics

private const val TAG = "GameFragment"

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class GameFragment : Fragment() {

    private lateinit var _binding: FragmentGameBinding
    private val _devicesViewModel: DevicesViewModel by viewModels()
    private lateinit var _surfaceView: SurfaceView
    private lateinit var _surfaceHolder: SurfaceHolder



    private val _testPoints = listOf<Point>(Point(100, 20),
            Point(45, 250),
            Point(70, 300))


    private val _players = 3

    private lateinit var _gattClient: GattClient



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        //Check is host im viewmodel ist set -> ansonsten ist man host
        if (false) {
//            val hostDevice = Device(_hostDevice.address, -70)
//            hostDevice.setName(_hostDevice.name)
//            _devicesViewModel.host = hostDevice
//            Log.d(TAG, "host: ${hostDevice.name} address: ${hostDevice.address}")
        }

        val gattClientCallback = GattClientCallback(_devicesViewModel)
        _gattClient = GattClient(requireContext())

        // connect
        //_gattClient.connect(_hostDevice, gattClientCallback)
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


        _surfaceView = _binding.surfaceView
        _surfaceHolder = _surfaceView.holder
        _surfaceHolder.addCallback(SurfaceCallback(_players, _testPoints))

        Log.i(TAG, "surface is valid: ${_surfaceHolder.surface.isValid}")

    }

    override fun onPause() {
        super.onPause()
        //_gattClient.disconnect()
    }

    override fun onResume() {
        super.onResume()
        //_gattClient.reconnect()
    }

}