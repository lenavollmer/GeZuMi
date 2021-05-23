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
import de.htw.gezumi.viewmodel.GameViewModel


private const val TAG = "SurfaceCallback"
private const val POINT_SIZE = 60f

class SurfaceCallback(
    private val _gameViewModel: GameViewModel,
    private val _context: Context,
    private val _viewLifecycleOwner: LifecycleOwner,
    private val _geometricObject: List<Point>
) :
    SurfaceHolder.Callback {

    private val _paints = Paints(_context)

    override fun surfaceChanged(
        holder: SurfaceHolder, format: Int,
        width: Int, height: Int
    ) {
        Log.d(TAG, "surfaceChanged")
    }


    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d(TAG, "surfaceCreated")
        // Create the observer which updates the UI.
        val nameObserver = Observer<List<Point>> { newLocations ->
            Log.d(TAG, "I am an observer and I do observe")
            tryDrawing(holder, newLocations)
        }

        // Observe the LiveData, passing in this activity as the LifecycleOwner and the observer.
        _gameViewModel.playerLocations.observe(_viewLifecycleOwner, nameObserver)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // and here you need to stop it
    }

    private fun tryDrawing(holder: SurfaceHolder, locations: List<Point>) {
        Log.d(TAG, "tryDrawing")
        Log.i(TAG, "Trying to draw... ${holder.isCreating}")


        val canvas = holder.lockCanvas()
        if (canvas == null) {
            Log.e(TAG, "Cannot draw onto the canvas as it's null")
        } else {
            drawMyStuff(canvas, locations)
            holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun drawMyStuff(canvas: Canvas, playerLocations: List<Point>) {
        Log.i(TAG, "Drawing...")

        // clear screen
        val backgroundColor = _context.getColorFromAttr(android.R.attr.windowBackground)
        canvas.drawColor(backgroundColor)

        // translate player location to target shape
        val targetShape = _geometricObject.map { Vec(it) }
        val players = playerLocations.map { Vec(it) }
        val translatedPlayers = players.map { it + targetShape[0] - players[0]}
        val base = targetShape[0]
        Log.i(TAG, "targetShape $targetShape")
        Log.i(TAG, "translatedPlayers $translatedPlayers")

        // rotate player locations to fit target shape
        val (_, indexLoc) = Geometry.getClockwisePoint(
            Pair(
                translatedPlayers[1] - base,
                translatedPlayers[2] - base
            )
        )
        val playersRightPoint = translatedPlayers[1 + indexLoc]
        Log.i(TAG, "players right $playersRightPoint")
        val (_, indexObj) = Geometry.getClockwisePoint(
            Pair(
                targetShape[1] - base,
                targetShape[2] - base
            )
        )
        val targetRightPoint = targetShape[1 + indexObj]
        Log.i(TAG, "target right $targetRightPoint")

        val angleToRotate = Geometry.getAngle(
            playersRightPoint - base,
            targetRightPoint - base
        )
        Log.i(TAG, "angle to rotate $angleToRotate")

        val allPoints = targetShape +
                Geometry.rotatePoints(
                    translatedPlayers, base, angleToRotate
                )
        Log.i(TAG, "final player ${allPoints.subList(3, 6)}")
        Log.i(TAG, "final target ${allPoints.subList(0, 3)}")

        // scale all points to fit canvas
        val points = Geometry.scaleToCanvas(
            allPoints.map { it.toPoint() },
            canvas.height,
            canvas.width,
            (POINT_SIZE * 2).toInt()
        )

        // draw player locations
        drawFigure(
            canvas,
            points.subList(0, 3),
            _paints.lineStroke,
            _paints.circleStroke,
            _paints.fillPaint
        )
        // draw target shape
        drawFigure(
            canvas,
            points.subList(3, 6),
            _paints.lineStrokeTargetShape,
            _paints.circleStrokeTargetShape,
            _paints.fillPaintTargetShape
        )
    }

    private fun drawFigure(
        canvas: Canvas,
        points: List<Point>,
        lineStroke: Paint,
        circleStroke: Paint,
        fillPaint: Paint
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
            canvas.drawCircle(x, y, POINT_SIZE, circleStroke)
            canvas.drawCircle(x, y, POINT_SIZE, fillPaint)
        }
    }
}