package de.htw.gezumi.canvas

import android.content.Context
import android.graphics.Paint
import android.util.TypedValue
import androidx.annotation.AttrRes
import androidx.annotation.ColorInt
import de.htw.gezumi.R

@ColorInt
fun Context.getColorFromAttr(
    @AttrRes attrColor: Int,
    typedValue: TypedValue = TypedValue(),
    resolveRefs: Boolean = true
): Int {
    theme.resolveAttribute(attrColor, typedValue, resolveRefs)
    return typedValue.data
}

private const val POINT_SIZE = 10f

class Paints(
    private val _context: Context
) {
    private val _colorAccent = _context.getColorFromAttr(R.attr.colorAccent)
    private val _colorPrimary = _context.getColorFromAttr(R.attr.colorPrimary)
    private val _backgroundColor = _context.getColorFromAttr(android.R.attr.windowBackground)

    val lineStroke = Paint().apply {
        isAntiAlias = true
        color = _colorAccent
        style = Paint.Style.STROKE
        strokeWidth = POINT_SIZE * 0.7f
    }
    val circleStroke = Paint().apply {
        isAntiAlias = true
        color = _colorAccent
        style = Paint.Style.STROKE
        strokeWidth = POINT_SIZE * 0.4f
    }
    val fillPaint = Paint().apply {
        isAntiAlias = true
        color = _backgroundColor
        style = Paint.Style.FILL
    }

    val lineStrokeTargetShape = Paint().apply {
        isAntiAlias = true
        color = _colorPrimary
        style = Paint.Style.STROKE
        strokeWidth = POINT_SIZE * 0.7f
    }
    val circleStrokeTargetShape= Paint().apply {
        isAntiAlias = true
        color = _colorPrimary
        style = Paint.Style.STROKE
        strokeWidth = POINT_SIZE * 0.4f
    }
    val fillPaintTargetShape = Paint().apply {
        isAntiAlias = true
        color = _backgroundColor
        style = Paint.Style.FILL
    }
}