package de.htw.gezumi.callbacks

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
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

        // TODO why do we need this observer? we already set game running false after the shapes matched
        val matchedObserver = Observer<Boolean> { shapesMatch ->
            if (shapesMatch) _gameViewModel.game.setRunning(false)
        }

        val animationObserver = Observer<List<Vec>> { animationLocation ->
            if (!_gameViewModel.game.running) {
                tryDrawing(holder) { canvas -> drawWinningShape(canvas, animationLocation) }
            }
        }

        val playerObserver = Observer<List<Player>> {
            if (_gameViewModel.game.running) {
                tryDrawing(holder) { canvas -> drawInGame(canvas, it) }
            }
        }

        // Observe the LiveData, passing in this activity as the LifecycleOwner and the observer.
        _gameViewModel.game.shapeMatched.observe(_viewLifecycleOwner, matchedObserver)
        _gameViewModel.game.targetShapeAnimation.observe(_viewLifecycleOwner, animationObserver)
        _gameViewModel.game.players.observe(_viewLifecycleOwner, playerObserver)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // and here you need to stop it <-- whats does that mean?
    }

    private fun tryDrawing(holder: SurfaceHolder, drawFunction: (canvas: Canvas) -> Unit) {
        Log.i(TAG, "Trying to draw... ${holder.isCreating}")
        val canvas = holder.lockCanvas()
        if (canvas == null) {
            Log.e(TAG, "Cannot draw onto the canvas as it's null")
        } else {
            drawFunction(canvas)
            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun drawInGame(canvas: Canvas, players: List<Player>) {
        var targetPositions = _gameViewModel.game.targetShape.value!!.toList()
        var playerPositions =
            players.filter { it.position != null }
                .map { it.position!! }.toList()

        Log.d(TAG, "targetPositions: $targetPositions")
        Log.i(TAG, "playerPositions: $playerPositions")

        if (playerPositions.size < 3 || targetPositions.size < 3) return

        val gamePositions = Geometry.arrangeGamePositions(
            playerPositions,
            targetPositions,
            canvas.height,
            canvas.width,
            (POINT_SIZE * 2).toInt()
        )
        playerPositions = gamePositions.playerPositions
        targetPositions = gamePositions.targetPositions


        val shapesMatch = Geometry.determineMatch(
            playerPositions,
            targetPositions
        )
        Log.d(TAG, "isMatch: $shapesMatch")

        clearCanvas(canvas)

        // draw target shape
        drawFigure(
            canvas,
            targetPositions,
            _paints.lineStrokeTargetShape,
            _paints.circleStrokeTargetShape,
            _paints.fillPaintTargetShape,
            POINT_SIZE * 1.2f
        )

        // draw players
        drawFigure(
            canvas,
            playerPositions,
            _paints.lineStroke,
            _paints.circleStroke,
            _paints.fillPaint,
            POINT_SIZE
        )
        drawPlayerNames(canvas, players, playerPositions, POINT_SIZE)

        if (shapesMatch) {
            _gameViewModel.game.generateTargetShapeAnimationPoints()
            _gameViewModel.game.setShapeMatched(shapesMatch)
            _gameViewModel.game.setRunning(false)
            Log.d(TAG, "animationShape: ${_gameViewModel.game.animationPointsArray.map { it }}")
        }

    }

    private fun drawWinningShape(canvas: Canvas, currentTargetShape: List<Vec>) {
        Log.d(TAG, "In drawWinningShape")

        clearCanvas(canvas)
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
            allPoints.subList(0, _gameViewModel.game.numberOfPlayers),
            _paints.lineStrokeTargetShapeSuccess,
            _paints.circleStrokeTargetShapeSuccess,
            _paints.fillPaintTargetShapeSuccess,
            POINT_SIZE * 1.2f
        )
    }

    private fun drawFigure(
        canvas: Canvas,
        points: List<Vec>,
        lineStroke: Paint,
        circleStroke: Paint,
        fillPaint: Paint,
        pointSize: Float
    ) {
        points.forEachIndexed { i, point ->
            val next = if (i < points.size - 1) points[i + 1] else points[0]
            canvas.drawLine(point.x, point.y, next.x, next.y, lineStroke)
        }

        points.forEach {
            canvas.drawCircle(it.x, it.y, pointSize, circleStroke)
            canvas.drawCircle(it.x, it.y, pointSize, fillPaint)
        }
    }

    private fun drawPlayerNames(
        canvas: Canvas,
        players: List<Player>,
        playerScaledPositions: List<Vec>,
        pointSize: Float
    ) {
        playerScaledPositions.forEachIndexed { i, position ->
            canvas.drawText(
                players[i].name.value!!,
                position.x,
                position.y - pointSize - 10f,
                _paints.textPaintPlayerName
            )
        }
    }

    private fun clearCanvas(canvas: Canvas) {
        val backgroundColor = _context.getColorFromAttr(android.R.attr.windowBackground)
        canvas.drawColor(backgroundColor)
    }
}