package de.htw.gezumi.callbacks

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
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

private const val POINT_SIZE = 60f

class SurfaceCallback(
    private val _gameViewModel: GameViewModel,
    _context: Context,
    private val _viewLifecycleOwner: LifecycleOwner,
) :
    SurfaceHolder.Callback {

    private val _painter = Painter(_context, POINT_SIZE)

    private var _canvasHeight: Int = 0
    private var _canvasWidth: Int = 0

    private var _oldGamePos: GamePositions? = null
    private var _animator: ValueAnimator? = null

    override fun surfaceCreated(holder: SurfaceHolder) {
        tryDrawing(holder) { canvas ->
            _canvasHeight = canvas.height
            _canvasWidth = canvas.width
        }

        val shapesMatchedObserver = Observer<Boolean> { if (it) animateWin(holder) }

        val playerObserver = Observer<List<Player>> {
            if (_gameViewModel.game.running) drawInGame(holder, it)
        }

        _gameViewModel.game.players.observe(_viewLifecycleOwner, playerObserver)
        _gameViewModel.game.shapeMatched.observe(_viewLifecycleOwner, shapesMatchedObserver)

    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
    }

    private fun tryDrawing(holder: SurfaceHolder, drawFunction: (canvas: Canvas) -> Unit) {
        val canvas = holder.lockCanvas()
        if (canvas != null) {
            drawFunction(canvas)
            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun drawInGame(holder: SurfaceHolder, players: List<Player>) {
        var gamePos = GamePositions(
            players.filter { it.position != null }
                .map { it.position!! }.toList(),
            _gameViewModel.game.targetShape.value!!.toList()
        )

        if (gamePos.players.size < 3 || gamePos.targets.size < 3) {
            _animator?.cancel()
            return
        }

        gamePos = Geometry.arrangeGamePositions(gamePos)

        if (_oldGamePos == null) _oldGamePos = gamePos

        _animator?.cancel()
        _animator = Animator.createVecAnimation(
            _oldGamePos!!.players + _oldGamePos!!.targets,
            gamePos.players + gamePos.targets
        ) { updatedVecs ->
            tryDrawing(holder) { canvas ->
                _oldGamePos = GamePositions(
                    updatedVecs.subList(0, gamePos.players.size),
                    updatedVecs.subList(gamePos.players.size, updatedVecs.size)
                )

                if (_oldGamePos!!.players.size >= 3 && _oldGamePos!!.targets.size >= 3 && players.size >= 3) {
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

                    val shapesMatch = Geometry.determineMatch(_oldGamePos!!)
                    if (shapesMatch) {
                        _animator?.cancel()
                        _gameViewModel.game.running = false
                        _gameViewModel.game.setShapeMatched(true)
                    }
                } else {
                    _animator?.cancel()
                }
            }
        }
    }

    private fun animateWin(holder: SurfaceHolder) {
        val center = Vec(_canvasWidth / 2, _canvasHeight / 2)

        val gamePos = Geometry.scaleGamePositions(
            _oldGamePos!!,
            _canvasHeight,
            _canvasWidth,
            (POINT_SIZE * 2).toInt()
        )

        val playerToTargetAnimator = Animator.createVecAnimation(
            gamePos.players,
            gamePos.players.map { player ->
                gamePos.targets.minByOrNull { target -> (target - player).length() }!!
            },
            500
        ) { updatedVecs ->
            tryDrawing(holder) { canvas ->
                _painter.clearCanvas(canvas)
                _painter.drawTargetShape(canvas, gamePos.targets)
                _painter.drawWinningFigure(canvas, updatedVecs)
            }
        }

        playerToTargetAnimator.doOnEnd {
            val scaleToCenterAnimator = Animator.createVecAnimation(
                gamePos.targets,
                gamePos.targets.map { center },
                800
            ) { updatedVecs ->
                tryDrawing(holder) { canvas ->
                    _painter.clearCanvas(canvas)
                    _painter.drawWinningFigure(canvas, updatedVecs)
                }
            }

            scaleToCenterAnimator.doOnEnd {
                Animator.createVecAnimation(
                    gamePos.targets.map { center },
                    gamePos.targets,
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