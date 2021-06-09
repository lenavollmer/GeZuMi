package de.htw.gezumi.callbacks

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.util.Log
import android.view.SurfaceHolder
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import de.htw.gezumi.calculation.Geometry
import de.htw.gezumi.calculation.Vec
import de.htw.gezumi.canvas.Paints
import de.htw.gezumi.canvas.getColorFromAttr
import de.htw.gezumi.model.Player
import de.htw.gezumi.viewmodel.GameViewModel
import java.util.*


private const val TAG = "SurfaceCallback"
private const val POINT_SIZE = 60f

class SurfaceCallback(
    private val _gameViewModel: GameViewModel,
    private val _context: Context,
    private val _viewLifecycleOwner: LifecycleOwner,
) :
    SurfaceHolder.Callback {

    private val _paints = Paints(_context, POINT_SIZE)

    override fun surfaceChanged(
        holder: SurfaceHolder, format: Int,
        width: Int, height: Int
    ) {
        Log.d(TAG, "surfaceChanged")
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d(TAG, "surfaceCreated")

        // Create the observer which updates the UI.
        val matchedObserver = Observer<Boolean> { shapesMatch ->
            if (shapesMatch) _gameViewModel.game.setRunning(false)
        }

        val animationObserver = Observer<List<Vec>> { animationLocation ->
            if (!_gameViewModel.game.running) tryDrawing(holder, animationLocation, true)
        }

        // TODO hier die Player direkt und nicht die Positionen übergeben
        val playerObserver = Observer<List<Player>> { players ->
            if (_gameViewModel.game.running) tryDrawing(
                holder,
                players.filter { it.position != null }.map { it.position!! },
                false
            )
        }


        // Observe the LiveData, passing in this activity as the LifecycleOwner and the observer.
        _gameViewModel.game.shapeMatched.observe(_viewLifecycleOwner, matchedObserver)
        _gameViewModel.game.targetShapeAnimation.observe(_viewLifecycleOwner, animationObserver)
        _gameViewModel.game.players.observe(_viewLifecycleOwner, playerObserver)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // and here you need to stop it
    }

    private fun tryDrawing(holder: SurfaceHolder, locations: List<Vec>, gameWon: Boolean) {
        Log.i(TAG, "Trying to draw... ${holder.isCreating}")

        val canvas = holder.lockCanvas()
        if (canvas == null) {
            Log.e(TAG, "Cannot draw onto the canvas as it's null")
        } else {
            if (gameWon) drawWinningShape(canvas, locations)
            // TODO hier die Player direkt und nicht die Positionen übergeben
            else drawMyStuff(canvas, locations)
            holder.unlockCanvasAndPost(canvas)
        }
    }


    private fun drawMyStuff(canvas: Canvas, playerLocations: List<Vec>) {
        var targetShape = Collections.unmodifiableList(_gameViewModel.game.targetShape.value!!)
        Log.d(TAG, "targetShape: $targetShape")
        Log.i(TAG, "playerLocations: $playerLocations")

        // TODO: playerlocations aus players rausholen

        if (playerLocations.size < 3 || targetShape.size < 3) return
        val playerCount = _gameViewModel.game.numberOfPlayers

        // clear screen.
        val backgroundColor = _context.getColorFromAttr(android.R.attr.windowBackground)
        canvas.drawColor(backgroundColor)

        // translate player location to target shape

        var players = playerLocations
        players = players.map { it + targetShape[0] - players[0] }
        val base = targetShape[0]

        // rotate player locations to fit target shape
        val (_, indexLoc) = Geometry.getClockwisePoint(
            Pair(
                players[1] - base,
                players[2] - base
            )
        )
        val playersRightPoint = players[1 + indexLoc]
        val (_, indexObj) = Geometry.getClockwisePoint(
            Pair(
                targetShape[1] - base,
                targetShape[2] - base
            )
        )
        val targetRightPoint = targetShape[1 + indexObj]

        val angleToRotate = Geometry.getAngleSigned(
            playersRightPoint - base,
            targetRightPoint - base
        )

        players = Geometry.rotatePoints(
            players, base, angleToRotate
        )

        // center players and target shape independently
        players = Geometry.center(players)
        targetShape = Geometry.center(targetShape)

        val shapesMatch = Geometry.determineMatch(
            players,
            targetShape
        )
        Log.d(TAG, "isMatch: $shapesMatch")

        // scale all points to fit canvas
        val allPoints = Geometry.scaleToCanvas(
            players + targetShape,
            canvas.height,
            canvas.width,
            (POINT_SIZE * 2).toInt()
        )

        // draw target shape
        drawFigure(
            canvas,
            allPoints.subList(playerCount, playerCount * 2).map { it.toPoint() },
            _paints.lineStrokeTargetShape,
            _paints.circleStrokeTargetShape,
            _paints.fillPaintTargetShape,
            POINT_SIZE * 1.2f
        )

        // draw player locations
        drawFigure(
            canvas,
            allPoints.subList(0, playerCount).map { it.toPoint() },
            _paints.lineStroke,
            _paints.circleStroke,
            _paints.fillPaint,
            POINT_SIZE
        )

        // TODO methode drawPlayers schreiben macht das gleiche wie drawFigures
        // TODO zusätlich sollen noch die Player namen gezeichnet werden

        if (shapesMatch) {
            _gameViewModel.game.generateTargetShapeAnimationPoints()
            _gameViewModel.game.setShapeMatched(shapesMatch)
            _gameViewModel.game.setRunning(false)
            Log.d(TAG, "animationShape: ${_gameViewModel.game.animationPointsArray.map { it }}")
        }

    }

    private fun drawWinningShape(canvas: Canvas, currentTargetShape: List<Vec>) {
        Log.d(TAG, "In drawWinningShape")

        // clear screen
        val backgroundColor = _context.getColorFromAttr(android.R.attr.windowBackground)
        canvas.drawColor(backgroundColor)

        // translate player location to target shape
        val allAnimationPoints = _gameViewModel.game.animationPointsArray.flatMap { it }

        // center players and target shape independently
        val targetShape = Geometry.center(currentTargetShape)
        val animationPoints = Geometry.center(allAnimationPoints)

        // scale all points to fit canvas
        val allPoints = Geometry.scaleToCanvas(
            targetShape + animationPoints,
            canvas.height,
            canvas.width,
            (POINT_SIZE * 2).toInt()
        )

        // draw and animate target shape
        drawFigure(
            canvas,
            allPoints.subList(0, _gameViewModel.game.numberOfPlayers).map { it.toPoint() },
            _paints.lineStrokeTargetShapeSuccess,
            _paints.circleStrokeTargetShapeSuccess,
            _paints.fillPaintTargetShapeSuccess,
            POINT_SIZE * 1.2f
        )
    }


    private fun drawFigure(
        canvas: Canvas,
        points: List<Point>,
        lineStroke: Paint,
        circleStroke: Paint,
        fillPaint: Paint,
        pointSize: Float
    ) {
        for (i in points.indices) {
            val current = points[i]
            val x = current.x.toFloat()
            val y = current.y.toFloat()

            // Draw line with next point (if it exists)
            if (i + 1 < points.size) {
                val next = points[i + 1]
                canvas.drawLine(x, y, next.x.toFloat(), next.y.toFloat(), lineStroke)
            }
        }

        canvas.drawLine(
            points[0].x.toFloat(),
            points[0].y.toFloat(),
            points[points.size - 1].x.toFloat(),
            points[points.size - 1].y.toFloat(),
            lineStroke
        )

        for (i in points.indices) {
            val current = points[i]
            val x = current.x.toFloat()
            val y = current.y.toFloat()

            // Draw points
            canvas.drawCircle(x, y, pointSize, circleStroke)
            canvas.drawCircle(x, y, pointSize, fillPaint)
        }
    }
}