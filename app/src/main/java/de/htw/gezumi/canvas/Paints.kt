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

class Paints(
    private val _context: Context,
    private val _pointSize: Float,

    ) {
    private val _colorAccent = _context.getColorFromAttr(R.attr.colorAccent)
    private val _backgroundColor = _context.getColorFromAttr(android.R.attr.windowBackground)
    private val _targetColor = _context.getColorFromAttr(R.attr.targetShapeColor)

    val lineStroke = Paint().apply {
        isAntiAlias = true
        color = _colorAccent
        style = Paint.Style.STROKE
        strokeWidth = _pointSize * 0.2f
    }
    val circleStroke = Paint().apply {
        isAntiAlias = true
        color = _colorAccent
        style = Paint.Style.STROKE
        strokeWidth = _pointSize * 0.1f
    }
    val fillPaint = Paint().apply {
        isAntiAlias = true
        color = _backgroundColor
        style = Paint.Style.FILL
    }

    val lineStrokeTargetShape = Paint().apply {
        isAntiAlias = true
        color = _targetColor
        style = Paint.Style.STROKE
        strokeWidth = _pointSize * 0.5f
    }
    val circleStrokeTargetShape= Paint().apply {
        isAntiAlias = true
        color = _targetColor
        style = Paint.Style.STROKE
        strokeWidth = _pointSize * 0.1f
    }
    val fillPaintTargetShape = Paint().apply {
        isAntiAlias = true
        color = _targetColor
        style = Paint.Style.FILL
    }
}