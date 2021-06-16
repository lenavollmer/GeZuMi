package de.htw.gezumi

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.Button
import androidx.core.os.bundleOf
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import de.htw.gezumi.callbacks.GameLeaveUICallback
import de.htw.gezumi.callbacks.SurfaceCallback
import de.htw.gezumi.databinding.FragmentGameBinding
import de.htw.gezumi.model.Player
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

    private var _firstLeave = true

    private val gameLeaveUICallback = object : GameLeaveUICallback {
        override fun gameLeft() {
            Handler(Looper.getMainLooper()).post {
                Log.d(TAG, "game ended by host")
                if(_firstLeave){
                    val bundle = bundleOf("gameEnded" to true)
                    findNavController().navigate(R.id.action_Game_to_MainMenuFragment, bundle)
                }
                _firstLeave = false
            }
        }
    }

    private val changeTargetLocations = object : Runnable {
        override fun run() {
            _gameViewModel.game.changeTargetLocationsLogic()
            mainHandler.postDelayed(this, 100)
        }
    }

    @kotlin.ExperimentalUnsignedTypes
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainHandler = Handler(Looper.getMainLooper())
    }

    @kotlin.ExperimentalUnsignedTypes
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_game, container, false)

        _gameViewModel.game.resetState()
        _gameViewModel.updateTargetShape()
        runTimer()

        val matchedObserver = Observer<Boolean> { shapesMatch ->
            if (shapesMatch) {
                _binding.shapesMatched.visibility = View.VISIBLE
                _binding.shapesMatched.z = 500.0F
                _binding.startNewGame.visibility = View.VISIBLE
                _binding.shapesMatched.z = 500.0F
            }
        }
        val playerObserver = Observer<List<Player>> { players ->
            if (players.size > 2 && _gameViewModel.game.targetShape.value!!.size > 2 ) {
                _binding.progressBar.visibility = View.INVISIBLE
                _binding.surfaceView.visibility = View.VISIBLE
            }
        }
        _gameViewModel.game.shapeMatched.observe(viewLifecycleOwner, matchedObserver)
        _gameViewModel.game.players.observe(viewLifecycleOwner, playerObserver)

        return _binding.root
    }

    @kotlin.ExperimentalUnsignedTypes
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding.lifecycleOwner = viewLifecycleOwner
        _binding.gameViewModel = _gameViewModel

        _gameViewModel.gameLeaveUICallback = gameLeaveUICallback

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
            _gameViewModel.updateTargetShape()
            _gameViewModel.game.resetState()
            _gameViewModel.game.setRunning(true)
        }

    }

    override fun onPause() {
        super.onPause()
        mainHandler.removeCallbacks(changeTargetLocations)
        _gameViewModel.game.setRunning(false)
    }

    override fun onResume() {
        super.onResume()
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
        mainHandler.removeCallbacks(changeTargetLocations)
        // stop scan and advertise
        if(_gameViewModel.isGattServerInitialized()){
            _gameViewModel.gattServer.notifyGameEnding()
            _gameViewModel.gattServer.stopServer()
        }
        if(_gameViewModel.isGattClientInitialized()) _gameViewModel.gattClient.disconnect()
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