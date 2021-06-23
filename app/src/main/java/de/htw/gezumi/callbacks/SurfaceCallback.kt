package de.htw.gezumi.callbacks

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.Log
import android.view.SurfaceHolder
import androidx.core.animation.doOnEnd
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import de.htw.gezumi.calculation.Geometry
import de.htw.gezumi.calculation.Vec
import de.htw.gezumi.canvas.Animator
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
    private var _canvasHeight: Int = 0
    private var _canvasWidth: Int = 0

    private var _oldPlayerPos: List<Vec>? = null
    private var _oldTargetPos: List<Vec>? = null

    private var _animator: ValueAnimator? = null

    override fun surfaceChanged(
        holder: SurfaceHolder, format: Int,
        width: Int, height: Int
    ) {
        Log.d(TAG, "surfaceChanged")
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d(TAG, "surfaceCreated")

        tryDrawing(holder) { canvas ->
            _canvasHeight = canvas.height
            _canvasWidth = canvas.width
        }

        val matchedObserver = Observer<Boolean> { shapesMatch ->
            if (shapesMatch) {
                _animator!!.cancel()
                animateWin(
                    _gameViewModel.game.players.value!!.map { it.position!! },
                    _gameViewModel.game.targetShape.value!!,
                    holder
                )
            }
        }

        val playerObserver = Observer<List<Player>> {
            if (_gameViewModel.game.running) {
                drawInGame(holder, it)
            }
        }

        _gameViewModel.game.players.observe(_viewLifecycleOwner, playerObserver)
        _gameViewModel.game.shapeMatched.observe(_viewLifecycleOwner, matchedObserver)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // and here you need to stop it <-- whats does that mean?
    }

    private fun tryDrawing(holder: SurfaceHolder, drawFunction: (canvas: Canvas) -> Unit) {
        val canvas = holder.lockCanvas()
        if (canvas == null) {
            Log.e(TAG, "Canvas is already locked")
        } else {
            drawFunction(canvas)
            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun drawInGame(holder: SurfaceHolder, players: List<Player>) {
        var targetPositions = _gameViewModel.game.targetShape.value!!.toList()
        var playerPositions =
            players.filter { it.position != null }
                .map { it.position!! }.toList()

        Log.i(TAG, "player positions: $playerPositions")

        if (playerPositions.size < 3 || targetPositions.size < 3) return

        val gamePositions = Geometry.arrangeGamePositions(
            playerPositions,
            targetPositions,
            _canvasHeight,
            _canvasWidth,
            (POINT_SIZE * 2).toInt()
        )
        playerPositions = gamePositions.playerPositions
        targetPositions = gamePositions.targetPositions


        val shapesMatch = Geometry.determineMatch(
            playerPositions,
            targetPositions
        )
        Log.d(TAG, "isMatch: $shapesMatch")

        if (_oldPlayerPos == null || _oldTargetPos == null) {
            _oldPlayerPos = playerPositions
            _oldTargetPos = targetPositions
        }

        _animator?.cancel()       
        _animator = Animator.createVecAnimation(
            _oldPlayerPos!! + _oldTargetPos!!,
            playerPositions + targetPositions
        ) { updatedVecs ->
            tryDrawing(holder) { canvas ->
                _oldPlayerPos = updatedVecs.subList(0, playerPositions.size)
                _oldTargetPos = updatedVecs.subList(playerPositions.size, updatedVecs.size)
                clearCanvas(canvas)

                // draw target shape
                drawFigure(
                    canvas,
                    _oldTargetPos!!,
                    _paints.lineStrokeTargetShape,
                    _paints.circleStrokeTargetShape,
                    _paints.fillPaintTargetShape,
                    POINT_SIZE * 1.2f
                )

                // draw players
                drawFigure(
                    canvas,
                    _oldPlayerPos!!,
                    _paints.lineStroke,
                    _paints.circleStroke,
                    _paints.fillPaint,
                    POINT_SIZE
                )
                drawPlayerNames(canvas, players, _oldPlayerPos!!, POINT_SIZE)
                Log.d(TAG, "players: $players")
                val playerSelfIndex = players.indexOfFirst { it.name.value == "" }
                drawPlayerSelf(canvas, _oldPlayerPos!![playerSelfIndex], POINT_SIZE)
            }
        }

        if (shapesMatch) {
            _gameViewModel.game.setShapeMatched(shapesMatch)
            _gameViewModel.game.setRunning(false)
        }
    }

    private fun animateWin(playerPositions: List<Vec>, targetPositions: List<Vec>, holder: SurfaceHolder) {
        val center = Vec(_canvasWidth / 2, _canvasHeight / 2)

        val arrangedPlayerPos = Geometry.arrangeGamePositions(
            playerPositions,
            targetPositions,
            _canvasHeight,
            _canvasWidth,
            (POINT_SIZE * 2).toInt()
        ).playerPositions

        val animator = Animator.createVecAnimation(
            arrangedPlayerPos,
            arrangedPlayerPos.map { center },
            1000
        ) { updatedVecs ->
            tryDrawing(holder) { canvas ->
                drawWinningFigure(canvas, updatedVecs)
            }
        }

        animator.doOnEnd {
            Animator.createVecAnimation(
                arrangedPlayerPos.map { center },
                arrangedPlayerPos,
                1000
            ) { updatedVecs ->
                tryDrawing(holder) { canvas ->
                    drawWinningFigure(canvas, updatedVecs)
                }
            }
        }
    }

    private fun drawWinningFigure(canvas: Canvas, positions: List<Vec>) {
        clearCanvas(canvas)
        drawFigure(
            canvas,
            positions,
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
            canvas.drawCircle(it.x, it.y, pointSize, fillPaint)
            canvas.drawCircle(it.x, it.y, pointSize, circleStroke)
        }
    }

    private fun drawPlayerNames(
        canvas: Canvas,
        players: List<Player>,
        playerScaledPositions: List<Vec>,
        pointSize: Float
    ) {
        playerScaledPositions.forEachIndexed { i, position ->
            val y = position.y - pointSize - 20f
            canvas.drawText(
                players[i].name.value!!,
                position.x,
                y,
                _paints.textPaintPlayerNameStroke
            )
            canvas.drawText(
                players[i].name.value!!,
                position.x,
                y,
                _paints.textPaintPlayerNameFill
            )
        }
    }

    private fun drawPlayerSelf(
        canvas: Canvas,
        playerSelfPosition: Vec,
        pointSize: Float
    ) {
        canvas.drawCircle(playerSelfPosition.x, playerSelfPosition.y, pointSize, _paints.playerSelfStroke)
    }

    private fun clearCanvas(canvas: Canvas) {
        val backgroundColor = _context.getColorFromAttr(android.R.attr.windowBackground)
        canvas.drawColor(backgroundColor)
    }
}