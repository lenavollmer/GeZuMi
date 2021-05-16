package de.htw.gezumi

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.util.Log
import android.view.SurfaceHolder

private const val TAG = "SurfaceCallback"

class SurfaceCallback(private val _players: Int, private val _testPoints: List<Point>) : SurfaceHolder.Callback {
    private val _paint = Paint().apply {
        isAntiAlias = true
        color = Color.YELLOW
        style = Paint.Style.FILL_AND_STROKE
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int,
                                width: Int, height: Int) {
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        tryDrawing(holder);
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // and here you need to stop it
    }

    private fun tryDrawing(holder: SurfaceHolder) {
        Log.i(TAG, "Trying to draw... ${holder.isCreating}");

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
        for (i in points.indices) {
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

        canvas.drawLine(points[0].x.toFloat(), points[0].y.toFloat(), points[points.size - 1].x.toFloat(), points[points.size - 1].y.toFloat(), paint)
    }

    private fun generateGeometricObject(players: Int): List<Point> {
        val generatedPoints = mutableListOf<Point>()
        for (i in 1..players) {
            generatedPoints.add(Point((0..250).random(), (0..400).random()))
        }

        return generatedPoints
    }
}