package de.htw.gezumi

import android.graphics.Point
import android.os.Bundle
import android.view.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import de.htw.gezumi.databinding.FragmentGameBinding
import de.htw.gezumi.callbacks.SurfaceCallback
import de.htw.gezumi.viewmodel.GameViewModel

private const val TAG = "GameFragment"

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class GameFragment : Fragment() {

    private val _gameViewModel: GameViewModel by activityViewModels()

    private lateinit var _binding: FragmentGameBinding


    private lateinit var _surfaceView: SurfaceView
    private lateinit var _surfaceHolder: SurfaceHolder



    private val _testPoints = listOf(Point(100, 20),
        Point(45, 250),
        Point(70, 300))


    // Set numbers of players = currently fixed to three
    private val _players = 3
    // bluetooth stuff also in game fragment or is it possible to manage all that in client and host?
    //private lateinit var _gattClient: GattClient
    //private lateinit var _hostDevice: BluetoothDevice

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        val hostDevice = Device(_hostDevice.address, -70, _hostDevice)
//        //hostDevice.setName(_hostDevice.name)
//        _gameViewModel.host = hostDevice
//        Log.d(TAG, "host: ${hostDevice.name} address: ${hostDevice.address}")
//        _gameViewModel.addDevice(hostDevice)
//
//        val gattClientCallback = GattClientCallback(_gameViewModel)
//        _gattClient = GattClient(requireContext())
//
//        // connect
//        _gattClient.connect(_hostDevice, gattClientCallback)
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

        _surfaceView = _binding.surfaceView
        _surfaceHolder = _surfaceView.holder
        _surfaceHolder.addCallback(SurfaceCallback(_players, _testPoints, context!!))

        Log.i(TAG, "surface is valid: ${_surfaceHolder.surface.isValid}")
    }

    override fun onPause() {
        super.onPause()
    //    _gameViewModel.writeRSSILog()
    //    _gattClient.disconnect()
    }

    override fun onResume() {
        super.onResume()
    //    _gattClient.reconnect()
    }

    override fun onStop() {
        super.onStop()
        // stop scan and advertise
        // TODO on resume has to start it again (but not twice!) -> implement pause/disconnect functionality
        _gameViewModel.onGameLeave()
    }
}