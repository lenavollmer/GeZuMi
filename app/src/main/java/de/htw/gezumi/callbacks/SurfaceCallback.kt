package de.htw.gezumi.callbacks

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.util.Log
import android.view.SurfaceHolder
import androidx.core.animation.doOnEnd
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import de.htw.gezumi.calculation.GamePositions
import de.htw.gezumi.calculation.Geometry
import de.htw.gezumi.calculation.Vec
import de.htw.gezumi.canvas.Animator
import de.htw.gezumi.canvas.Painter
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

    private val _painter = Painter(_context, POINT_SIZE)

    private var _canvasHeight: Int = 0
    private var _canvasWidth: Int = 0

    private var _oldGamePos: GamePositions? = null
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

        if (playerPositions.size < 3 || targetPositions.size < 3) return

        val gamePos = Geometry.arrangeGamePositions(
            playerPositions,
            targetPositions,
        )

        if (_oldGamePos == null) {
            _oldGamePos = gamePos
        }

        _animator?.cancel()
        _animator = Animator.createVecAnimation(
            _oldGamePos!!.players + _oldGamePos!!.targets,
            gamePos.players + gamePos.targets
        ) { updatedVecs ->
            tryDrawing(holder) { canvas ->
                _oldGamePos = GamePositions(
                    updatedVecs.subList(0, playerPositions.size),
                    updatedVecs.subList(playerPositions.size, updatedVecs.size)
                )
                val scaledGamePos = Geometry.scaleGamePositions(
                    _oldGamePos!!,
                    _canvasHeight,
                    _canvasWidth,
                    (POINT_SIZE * 2).toInt()
                )

                _painter.clearCanvas(canvas)
                _painter.drawTargetShape(canvas, scaledGamePos.targets)
                _painter.drawPlayerFigure(canvas, scaledGamePos.players)
                _painter.drawPlayerNames(canvas, players, scaledGamePos.players)

                val playerSelfIndex = players.indexOfFirst { it.name.value == "" }
                _painter.drawPlayerSelf(canvas, scaledGamePos.players[playerSelfIndex])

                val shapesMatch = Geometry.determineMatch(_oldGamePos!!.players, _oldGamePos!!.targets)
                if (shapesMatch) {
                    _animator?.cancel()
                    _gameViewModel.game.setRunning(false)
                    _gameViewModel.game.setShapeMatched(true)
                }
            }
        }
    }

    private fun animateWin(holder: SurfaceHolder) {
        val center = Vec(_canvasWidth / 2, _canvasHeight / 2)

        val scaledGamePos = Geometry.scaleGamePositions(
            _oldGamePos!!,
            _canvasHeight,
            _canvasWidth,
            (POINT_SIZE * 2).toInt()
        )

        val playerToTargetAnimator = Animator.createVecAnimation(
            scaledGamePos.players,
            scaledGamePos.players.map { player ->
                scaledGamePos.targets.minByOrNull { target -> (target - player).length() }!!
            },
            500
        ) { updatedVecs ->
            tryDrawing(holder) { canvas ->
                _painter.clearCanvas(canvas)
                _painter.drawTargetShape(canvas, scaledGamePos.targets)
                _painter.drawWinningFigure(canvas, updatedVecs)
            }
        }

        playerToTargetAnimator.doOnEnd {
            val scaleToCenterAnimator = Animator.createVecAnimation(
                scaledGamePos.targets,
                scaledGamePos.targets.map { center },
                800
            ) { updatedVecs ->
                tryDrawing(holder) { canvas ->
                    _painter.clearCanvas(canvas)
                    _painter.drawWinningFigure(canvas, updatedVecs)
                }
            }

            scaleToCenterAnimator.doOnEnd {
                Animator.createVecAnimation(
                    scaledGamePos.targets.map { center },
                    scaledGamePos.targets,
                    800
                ) { updatedVecs ->
                    tryDrawing(holder) { canvas ->
                        _painter.clearCanvas(canvas)
                        _painter.drawWinningFigure(canvas, updatedVecs)
                    }
                }
            }
        }
        _oldGamePos = null
    }
}