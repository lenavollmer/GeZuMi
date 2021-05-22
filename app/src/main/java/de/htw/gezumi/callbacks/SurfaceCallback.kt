package de.htw.gezumi.callbacks

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.Drawable
import android.util.Log
import android.util.TypedValue
import android.view.SurfaceHolder
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import de.htw.gezumi.R
import de.htw.gezumi.calculation.Geometry


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
    private val _players: Int,
    private val _testPoints: List<Point>,
    private val _context: Context
) :
    SurfaceHolder.Callback {

    override fun surfaceChanged(
        holder: SurfaceHolder, format: Int,
        width: Int, height: Int
    ) {
        Log.d(TAG, "surfaceChanged::$_testPoints")
        tryDrawing(holder);
    }


    override fun surfaceCreated(holder: SurfaceHolder) {
        Log.d(TAG, "surfaceCreated::$_testPoints")
        tryDrawing(holder);
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        // and here you need to stop it
    }

    fun tryDrawing(holder: SurfaceHolder) {
        Log.d(TAG, "tryDrawing::$_testPoints")
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
        val accentColor = _context.getColorFromAttr(R.attr.colorPrimary)
        val backgroundColor = _context.getColorFromAttr(R.attr.backgroundColor)


        // Clear screen
        canvas.drawColor(backgroundColor);

        val lineStroke = Paint().apply {
            isAntiAlias = true
            color = accentColor
            style = Paint.Style.STROKE
        }
        lineStroke.strokeWidth = POINT_SIZE * 0.7f

        val circleStroke = Paint().apply {
            isAntiAlias = true
            color = accentColor
            style = Paint.Style.STROKE
        }
        circleStroke.strokeWidth = POINT_SIZE * 0.4f

        val fillPaint = Paint().apply {
            isAntiAlias = true
            color = backgroundColor
            style = Paint.Style.FILL
        }

        drawFigure(
            canvas,
            Geometry.scaleToCanvas(
                _testPoints,
//                generateGeometricObject(3),
                canvas.height,
                canvas.width,
                (POINT_SIZE * 2).toInt(),
            ),
            lineStroke,
            circleStroke,
            fillPaint
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