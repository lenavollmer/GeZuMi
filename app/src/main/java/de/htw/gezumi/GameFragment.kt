package de.htw.gezumi

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
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
                Log.d(TAG, "game terminated by host")
                if (_firstLeave) {
                    val bundle = bundleOf("gameEnded" to true)
                    findNavController().navigate(R.id.action_Game_to_MainMenuFragment, bundle)
                }
                _firstLeave = false
            }
        }
    }

    private val timer = object : Runnable {
        override fun run() {
            val seconds = _gameViewModel.game.time
            val hours = seconds / 3600
            val minutes = seconds % 3600 / 60
            val secs = seconds % 60

            // Format time
            val time: String = java.lang.String
                .format(
                    Locale.getDefault(),
                    "%d:%02d:%02d", hours,
                    minutes, secs
                )

            _binding.timeView.text = time

            // If running is true, increment the
            // seconds variable.
            if (_gameViewModel.game.running) {
                _gameViewModel.game.time++
            }

            // Post the code again
            // with a delay of 1 second.
            mainHandler.postDelayed(this, 1000)
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

        _gameViewModel.updateAndSendTargetShape()

        val matchedObserver = Observer<Boolean> { shapesMatch ->
            if (shapesMatch) {
                _binding.shapesMatched.visibility = View.VISIBLE
                _binding.shapesMatched.z = 500.0F
                if(_gameViewModel.isHost()) _binding.startNewGame.visibility = View.VISIBLE
                _binding.shapesMatched.z = 500.0F
                mainHandler.removeCallbacks(timer)
            } else {
                _binding.shapesMatched.visibility = View.INVISIBLE
                if(_gameViewModel.isHost()) _binding.startNewGame.visibility = View.INVISIBLE
            }
        }
        val playerObserver = Observer<List<Player>> { players ->
            val playersWithPosition =
                players.filter { it.position != null && !it.position!!.isNan() }
            if (
                playersWithPosition.size > 2 &&
                _gameViewModel.game.targetShape.value!!.size > 2 &&
                !_gameViewModel.game.running
            ) {
                if (!_gameViewModel.game.shapeMatched.value!!){
                    _gameViewModel.game.running = true
                    mainHandler.post(timer)
                }
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

        view.findViewById<Button>(R.id.start_new_game).setOnClickListener {
            _binding.progressBar.visibility = View.VISIBLE
            _gameViewModel.game.restart()
            _gameViewModel.notifyGameRestart()
            _gameViewModel.updateAndSendTargetShape()
        }

        (activity as AppCompatActivity?)!!.supportActionBar?.hide() // Hide Appbar

    }

    @kotlin.ExperimentalUnsignedTypes
    @SuppressLint("DefaultLocale")
    override fun onStop() {
        super.onStop()

        // stop scan and advertise
        if (_gameViewModel.isGattServerInitialized()) {
            _gameViewModel.gattServer.notifyGameEnding()
            _gameViewModel.gattServer.stopServer()
        }
        if (_gameViewModel.isGattClientInitialized()) _gameViewModel.gattClient.disconnect()
        _gameViewModel.bluetoothController.stopAdvertising()

        mainHandler.removeCallbacks(timer)
        _gameViewModel.game.reset()
        _gameViewModel.clearModel()
    }
}