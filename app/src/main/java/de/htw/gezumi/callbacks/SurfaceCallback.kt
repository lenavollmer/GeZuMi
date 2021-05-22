package de.htw.gezumi.callbacks

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.util.Log
import android.util.TypedValue
import android.view.SurfaceHolder
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import de.htw.gezumi.R
import de.htw.gezumi.calculation.Geometry
import de.htw.gezumi.viewmodel.GameViewModel


private const val TAG = "SurfaceCallback"
private const val POINT_SIZE = 60f

@ColorInt
fun Context.getColorFromAttr(
    @AttrRes attrColor: Int,
    typedValue: TypedValue = TypedValue(),
    resolveRefs: Boolean = true
): Int {
    theme.resolveAttribute(attrColor, typedValue, resolveRefs)
    return typedValue.data
}

class SurfaceCallback(
    private val _gameViewModel: GameViewModel,
    private val _context: Context,
    private val _viewLifecycleOwner: LifecycleOwner,
    private val _geometricObject: List<Point>
) :
    SurfaceHolder.Callback {

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
            tryDrawing(holder, newLocations);
        }

        // Observe the LiveData, passing in this activity as the LifecycleOwner and the observer.
        _gameViewModel.playerLocations.observe(_viewLifecycleOwner, nameObserver)
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // and here you need to stop it
    }

    fun tryDrawing(holder: SurfaceHolder, locations: List<Point>) {
        Log.d(TAG, "tryDrawing")
        Log.i(TAG, "Trying to draw... ${holder.isCreating}");


        val canvas = holder.lockCanvas();
        if (canvas == null) {
            Log.e(TAG, "Cannot draw onto the canvas as it's null");
        } else {
            drawMyStuff(canvas, locations)
            holder.unlockCanvasAndPost(canvas);
        }
    }

    private fun drawMyStuff(canvas: Canvas, locations: List<Point>) {
        Log.i(TAG, "Drawing...");
        val colorAccent = _context.getColorFromAttr(R.attr.colorAccent)
        val colorPrimary = _context.getColorFromAttr(R.attr.colorPrimary)

        val backgroundColor = _context.getColorFromAttr(android.R.attr.windowBackground)

        // Clear screen
        canvas.drawColor(backgroundColor);

        val lineStroke = Paint().apply {
            isAntiAlias = true
            color = colorAccent
            style = Paint.Style.STROKE
            strokeWidth = POINT_SIZE * 0.7f
        }
        val circleStroke = Paint().apply {
            isAntiAlias = true
            color = colorAccent
            style = Paint.Style.STROKE
            strokeWidth = POINT_SIZE * 0.4f
        }
        val fillPaint = Paint().apply {
            isAntiAlias = true
            color = backgroundColor
            style = Paint.Style.FILL
        }

        val lineStrokeGeometricObj = Paint().apply {
            isAntiAlias = true
            color = colorPrimary
            style = Paint.Style.STROKE
            strokeWidth = POINT_SIZE * 0.7f
        }
        val circleStrokeGeometricObj = Paint().apply {
            isAntiAlias = true
            color = colorPrimary
            style = Paint.Style.STROKE
            strokeWidth = POINT_SIZE * 0.4f
        }
        val fillPaintGeometricObj = Paint().apply {
            isAntiAlias = true
            color = backgroundColor
            style = Paint.Style.FILL
        }

        val points = Geometry.scaleToCanvas(
            _geometricObject + Geometry.translateToOverlay(locations, _geometricObject),
            canvas.height,
            canvas.width,
            (POINT_SIZE * 2).toInt()
        )



        drawFigure(
            canvas,
            points.subList(0,3),
            lineStroke,
            circleStroke,
            fillPaint
        )

        drawFigure(
            canvas,
            points.subList(3,6),
            lineStrokeGeometricObj,
            circleStrokeGeometricObj,
            fillPaintGeometricObj
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
                canvas.drawLine(x, y, next.x.toFloat(), next.y.toFloat(), lineStroke);
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
            canvas.drawCircle(x, y, POINT_SIZE, circleStroke);
            canvas.drawCircle(x, y, POINT_SIZE, fillPaint);
        }
    }

//    private fun generateGeometricObject(players: Int): List<Point> {
//        val generatedPoints = mutableListOf<Point>()
//        for (i in 1..players) {
//            generatedPoints.add(Point((0..250).random(), (0..400).random()))
//        }
//
//        return generatedPoints
//    }
}