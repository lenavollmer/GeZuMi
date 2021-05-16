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

private const val TAG = "GameFragment"

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class GameFragment : Fragment() {

    private lateinit var _binding: FragmentGameBinding
    private val _devicesViewModel: DevicesViewModel by viewModels()
    private lateinit var _surfaceView: SurfaceView
    private lateinit var _surfaceHolder: SurfaceHolder
    private val _paint = Paint(Paint.ANTI_ALIAS_FLAG)


    private val _testPoints = listOf<Point>(Point(1, 2),
            Point(4, 2),
            Point(1, 3))


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

        tryDrawing(_surfaceHolder)
    }

    override fun onPause() {
        super.onPause()
        //_gattClient.disconnect()
    }

    override fun onResume() {
        super.onResume()
        //_gattClient.reconnect()
    }

    private fun tryDrawing(holder: SurfaceHolder) {
        Log.i(TAG, "Trying to draw...");

        val canvas = holder.lockCanvas();
        if (canvas == null) {
            Log.e(TAG, "Cannot draw onto the canvas as it's null");
        } else {
            drawMyStuff(canvas);
            holder.unlockCanvasAndPost(canvas);
        }
    }

    private fun drawMyStuff(canvas: Canvas) {
        Log.i(TAG, "Drawing...");

        // Clear screen
        canvas.drawColor(Color.BLACK);

        // Iterate on the list of generated points
        val generatedPoints = generateGeometricObject(_players)
        drawFigures(canvas, generatedPoints, _paint)

        // Iterate on the list of player locations
        val playerPaint = Paint().apply {
            isAntiAlias = true
            color = Color.RED
            style = Paint.Style.STROKE
        }
        drawFigures(canvas, _testPoints, playerPaint)
    }

    private fun drawFigures(canvas: Canvas, points: List<Point>, paint: Paint) {
        for (i in 0..points.size) {
            val current = points[i]
            val x = current.x.toFloat()
            val y = current.y.toFloat()

            // Draw points
            canvas.drawCircle(x, y, 10F, paint);

            // Draw line with next point (if it exists)
            if (i + 1 < points.size) {
                val next = points[i + 1]
                canvas.drawLine(x, y, next.x.toFloat(), next.y.toFloat(), paint);
            }
        }
    }

    private fun generateGeometricObject(players: Int): List<Point> {
        val generatedPoints = mutableListOf<Point>()
        for (i in 1..players) {
            generatedPoints.add(Point((0..10).random(), (0..10).random()))
        }

        return generatedPoints
    }
}