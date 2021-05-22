package de.htw.gezumi

import android.graphics.Point
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.*
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.Observer
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
    lateinit var mainHandler: Handler


    private lateinit var _surfaceView: SurfaceView
    private lateinit var _surfaceHolder: SurfaceHolder





    // Set numbers of players = currently fixed to three
    private val _players = 3
    // bluetooth stuff also in game fragment or is it possible to manage all that in client and host?
    //private lateinit var _gattClient: GattClient
    //private lateinit var _hostDevice: BluetoothDevice



    private val updateTextTask = object : Runnable {
        override fun run() {
            _gameViewModel.setPlayerLocations(generateGeometricObject(_players))
            Log.d(TAG, "locations: ${_gameViewModel.playerLocations}")
            mainHandler.postDelayed(this, 1000)
        }
    }

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
        mainHandler = Handler(Looper.getMainLooper())
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
        _surfaceHolder.addCallback(SurfaceCallback(_players, _gameViewModel.playerLocations.value!!, requireContext()))

        Log.i(TAG, "surface is valid: ${_surfaceHolder.surface.isValid}")


        // Create the observer which updates the UI.
        val nameObserver = Observer<List<Point>> { newLocations ->
            Log.d(TAG, "I am an observer and I do observe")
            _surfaceHolder.addCallback(SurfaceCallback(_players, newLocations, requireContext()))
        }

        // Observe the LiveData, passing in this activity as the LifecycleOwner and the observer.
        _gameViewModel.playerLocations.observe(viewLifecycleOwner, nameObserver)
    }

    override fun onPause() {
        super.onPause()
        //    _gameViewModel.writeRSSILog()
        //    _gattClient.disconnect()
        mainHandler.removeCallbacks(updateTextTask)
    }

    override fun onResume() {
        super.onResume()
        //    _gattClient.reconnect()
        mainHandler.post(updateTextTask)
    }

    override fun onStop() {
        super.onStop()
        // stop scan and advertise
        // TODO on resume has to start it again (but not twice!) -> implement pause/disconnect functionality
        _gameViewModel.onGameLeave()
    }

    private fun generateGeometricObject(players: Int): List<Point> {
        val generatedPoints = mutableListOf<Point>()
        for (i in 1..players) {
            generatedPoints.add(Point((0..250).random(), (0..400).random()))
        }

        return generatedPoints
    }
}