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
import de.htw.gezumi.calculation.Geometry
import de.htw.gezumi.calculation.Vec
import de.htw.gezumi.callbacks.SurfaceCallback
import de.htw.gezumi.databinding.FragmentGameBinding
import de.htw.gezumi.viewmodel.GameViewModel
import java.util.*


private const val TAG = "MockedGameFragment"

class MockedGameFragment : Fragment() {

    private val _gameViewModel: GameViewModel by activityViewModels()

    private lateinit var _binding: FragmentGameBinding
    lateinit var mainHandler: Handler


    private lateinit var _surfaceView: SurfaceView
    private lateinit var _surfaceHolder: SurfaceHolder

    private var initializedPlayerNames = false

    private val changePlayerLocations = object : Runnable {
        val player1 = byteArrayOf(0, 0, 0)
        val player2 = byteArrayOf(1, 1, 1)
        val player3 = byteArrayOf(2, 2, 2)
        val playerNames = arrayOf("", "Targo", "Kaenu")

        override fun run() {
            Log.d(TAG, "${_gameViewModel.game.time}")
            if (!_gameViewModel.game.shapeMatched.value!!) {
                val currentObj = Geometry.generateGeometricObject(3)
                _gameViewModel.game.updatePlayer(player1, currentObj[0])
                _gameViewModel.game.updatePlayer(player2, currentObj[1])
                _gameViewModel.game.updatePlayer(player3, currentObj[2])
                if (!initializedPlayerNames) {
                    _gameViewModel.game.players.value!!.forEachIndexed { index, player ->
                        player.setName(playerNames[index])
                    }
                }
            }
            mainHandler.postDelayed(this, 2000)
        }
    }

    @kotlin.ExperimentalUnsignedTypes
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val targetShape = Geometry.generateGeometricObject(3)
        _gameViewModel.game.setTargetShape(targetShape as MutableList<Vec>)
        mainHandler = Handler(Looper.getMainLooper())
    }

    @kotlin.ExperimentalUnsignedTypes
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_game, container, false)

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

    @kotlin.ExperimentalUnsignedTypes
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding.lifecycleOwner = viewLifecycleOwner
        _binding.gameViewModel = _gameViewModel

        _binding.progressBar.visibility = View.INVISIBLE
        _binding.surfaceView.visibility = View.VISIBLE

        _surfaceView = _binding.surfaceView
        _surfaceHolder = _surfaceView.holder
        _surfaceHolder.addCallback(
            SurfaceCallback(
                _gameViewModel,
                requireContext(),
                viewLifecycleOwner
            )
        )
        _gameViewModel.game.running = true

        view.findViewById<Button>(R.id.start_new_game).setOnClickListener {
            _binding.shapesMatched.visibility = View.INVISIBLE
            _binding.startNewGame.visibility = View.INVISIBLE
            _gameViewModel.game.restart()
        }

        runTimer()
    }

    override fun onPause() {
        super.onPause()
        mainHandler.removeCallbacks(changePlayerLocations)
        _gameViewModel.game.running = false
    }

    override fun onResume() {
        super.onResume()
        mainHandler.post(changePlayerLocations)
        _gameViewModel.game.running = true
    }

    @kotlin.ExperimentalUnsignedTypes
    @SuppressLint("DefaultLocale")
    override fun onStop() {
        super.onStop()
        _gameViewModel.game.running = false
        _gameViewModel.game.setShapeMatched(false)
        mainHandler.removeCallbacks(changePlayerLocations)
        // stop scan and advertise
        if (_gameViewModel.isGattServerInitialized()) {
            _gameViewModel.gattServer.notifyGameEnding()
            _gameViewModel.gattServer.stopServer()
        }
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
                    _gameViewModel.game.time = seconds + 1
                }

                // Post the code again
                // with a delay of 1 second.
                mainHandler.postDelayed(this, 1000)
            }
        })
    }
}