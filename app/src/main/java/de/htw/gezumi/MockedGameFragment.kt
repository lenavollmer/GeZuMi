package de.htw.gezumi

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
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


private const val TAG = "MockedGameFragment"

class MockedGameFragment : Fragment() {

    private val _gameViewModel: GameViewModel by activityViewModels()

    private lateinit var _binding: FragmentGameBinding
    lateinit var mainHandler: Handler


    private lateinit var _surfaceView: SurfaceView
    private lateinit var _surfaceHolder: SurfaceHolder

    private val changeTargetLocations = object : Runnable {
        override fun run() {
            _gameViewModel.game.changeTargetLocationsLogic()
            mainHandler.postDelayed(this, 100)
        }
    }

    // TODO remove generating random player location
    private val changePlayerLocations = object : Runnable {
        val player1 = byteArrayOf(0,0,0)
        val player2 = byteArrayOf(1,1,1)
        val player3 = byteArrayOf(2,2,2)

        override fun run() {
            if (_gameViewModel.game.time < 5) {
                val currentObj = Geometry.generateGeometricObject(3)
                _gameViewModel.game.updatePlayer(player1, currentObj[0])
                _gameViewModel.game.updatePlayer(player2, currentObj[1])
                _gameViewModel.game.updatePlayer(player3, currentObj[2])
            } else {
                _gameViewModel.game.updatePlayer(player1, _gameViewModel.game.targetShape.value!![0])
                _gameViewModel.game.updatePlayer(player2, _gameViewModel.game.targetShape.value!![1])
                _gameViewModel.game.updatePlayer(player3, _gameViewModel.game.targetShape.value!![2])
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
        _gameViewModel.game.resetState()

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
        mainHandler.removeCallbacks(changePlayerLocations)
        mainHandler.removeCallbacks(changeTargetLocations)
        _gameViewModel.game.setRunning(false)
    }

    override fun onResume() {
        super.onResume()
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
        if (_gameViewModel.isGattServerInitialized()) {
            _gameViewModel.gattServer.notifyGameEnding()
            _gameViewModel.gattServer.stopServer()
        }
    }
}