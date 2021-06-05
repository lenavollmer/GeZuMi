package de.htw.gezumi

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.Button
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import de.htw.gezumi.callbacks.SurfaceCallback
import de.htw.gezumi.databinding.FragmentGameBinding
import de.htw.gezumi.viewmodel.GameViewModel
import java.util.*


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


    // bluetooth stuff also in game fragment or is it possible to manage all that in client and host?
    //private lateinit var _gattClient: GattClient
    //private lateinit var _hostDevice: BluetoothDevice


    // TODO remove generating random player location
    private val changePlayerLocations = object : Runnable {
//        val player1 = byteArrayOf()
//        val player2 = byteArrayOf()
//        val player3 = byteArrayOf()

        override fun run() {
//            if(_gameViewModel.game.time < 5) {
//                val currentObj = Geometry.generateGeometricObject(_gameViewModel.game.numberOfPlayers)
//                _gameViewModel.game.updatePlayer(player1, Vec(currentObj[0]))
//                _gameViewModel.game.updatePlayer(player2, Vec(currentObj[1]))
//                _gameViewModel.game.updatePlayer(player3, Vec(currentObj[2]))
//            }
//            else {
//                _gameViewModel.game.updatePlayer(player1, _gameViewModel.game.targetShape[0])
//                _gameViewModel.game.updatePlayer(player2, _gameViewModel.game.targetShape[1])
//                _gameViewModel.game.updatePlayer(player3, _gameViewModel.game.targetShape[2])
//            }
            mainHandler.postDelayed(this, 2000)
        }
    }

    private val changeTargetLocations = object : Runnable {
        override fun run() {
            _gameViewModel.game.changeTargetLocationsLogic()
            mainHandler.postDelayed(this, 100)
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

        _gameViewModel.game.resetState()
        runTimer()

        val matchedObserver = Observer<Boolean> { shapesMatch ->
            if (shapesMatch) {
                _binding.shapesMatched.visibility = View.VISIBLE
                _binding.shapesMatched.z = 500.0F
                _binding.startNewGame.visibility = View.VISIBLE
                _binding.shapesMatched.z = 500.0F
            }
        }

        // Observe the LiveData, passing in this activity as the LifecycleOwner and the observer.
        _gameViewModel.game.shapeMatched.observe(viewLifecycleOwner, matchedObserver)

        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding.lifecycleOwner = viewLifecycleOwner
        _binding.gameViewModel = _gameViewModel

        _surfaceView = _binding.surfaceView
        _surfaceHolder = _surfaceView.holder
        _surfaceHolder.addCallback(
            SurfaceCallback(
                _gameViewModel,
                requireContext(),
                viewLifecycleOwner
            )
        )
        _gameViewModel.game.setRunning(true)

        view.findViewById<Button>(R.id.start_new_game).setOnClickListener {
            _binding.shapesMatched.visibility = View.INVISIBLE
            _binding.startNewGame.visibility = View.INVISIBLE
            _gameViewModel.game.resetState()
            _gameViewModel.game.setRunning(true)
        }

    }

    override fun onPause() {
        super.onPause()
        //    _gameViewModel.writeRSSILog()
        //    _gattClient.disconnect()
        mainHandler.removeCallbacks(changePlayerLocations)
        mainHandler.removeCallbacks(changeTargetLocations)
        _gameViewModel.game.setRunning(false)
    }

    override fun onResume() {
        super.onResume()
        //    _gattClient.reconnect()
        mainHandler.post(changePlayerLocations)
        mainHandler.post(changeTargetLocations)
        _gameViewModel.game.setRunning(true)
    }

    @kotlin.ExperimentalUnsignedTypes
    @SuppressLint("DefaultLocale")
    override fun onStop() {
        super.onStop()
        _gameViewModel.game.setRunning(false)
        _gameViewModel.game.setShapeMatched(false)
        _gameViewModel.game.resetCurrentIdx()
        mainHandler.removeCallbacks(changePlayerLocations)
        mainHandler.removeCallbacks(changeTargetLocations)
        // stop scan and advertise
        // TODO on resume has to start it again (but not twice!) -> implement pause/disconnect functionality
        _gameViewModel.onGameLeave()
    }

    private fun runTimer() {
        // Get the text view.
        val timeView = _binding.timeView

        mainHandler.post(object : Runnable {
            override fun run() {
                val seconds = _gameViewModel.game.time
                val hours = seconds / 3600
                val minutes = seconds % 3600 / 60
                val secs = seconds % 60

                // Format the seconds into hours, minutes,
                // and seconds.
                val time: String = java.lang.String
                    .format(
                        Locale.getDefault(),
                        "%d:%02d:%02d", hours,
                        minutes, secs
                    )

                timeView.text = time

                // If running is true, increment the
                // seconds variable.
                if (_gameViewModel.game.running) {
                    _gameViewModel.game.setTime(seconds + 1)
                }

                // Post the code again
                // with a delay of 1 second.
                mainHandler.postDelayed(this, 1000)
            }
        })
    }
}