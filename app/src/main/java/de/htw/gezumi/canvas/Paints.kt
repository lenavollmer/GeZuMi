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
    _context: Context,
    private val _pointSize: Float,
    ) {
    private val _colorPrimary = _context.getColorFromAttr(R.attr.colorPrimary)
    private val _targetColor = _context.getColorFromAttr(R.attr.targetShapeColor)
    private val _textPaintPlayerName = _context.getColorFromAttr(R.attr.playerNameColor)
    private val _targetSuccessColor = _colorPrimary
    private val _typeface = _context.resources.getFont(R.font.roboto_mono)
    private val _playerSelfColor = _context.getColorFromAttr(R.attr.playerSelfColor)
    
    val backgroundColor = _context.getColorFromAttr(android.R.attr.windowBackground)

    val lineStroke = Paint().apply {
        isAntiAlias = true
        color = _colorPrimary
        style = Paint.Style.STROKE
        strokeWidth = _pointSize * 0.4f
    }
    val circleStroke = Paint().apply {
        isAntiAlias = true
        color = _colorPrimary
        style = Paint.Style.STROKE
        strokeWidth = _pointSize * 0.2f
    }
    val playerSelfStroke = Paint().apply {
        isAntiAlias = true
        color = _playerSelfColor
        style = Paint.Style.STROKE
        strokeWidth = _pointSize * 0.2f
    }
    val fillPaint = Paint().apply {
        isAntiAlias = true
        color = backgroundColor
        style = Paint.Style.FILL
    }

    val lineStrokeTargetShape = Paint().apply {
        isAntiAlias = true
        color = _targetColor
        style = Paint.Style.STROKE
        strokeWidth = _pointSize * 0.5f
    }
    val circleStrokeTargetShape = Paint().apply {
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

    val lineStrokeTargetShapeSuccess = Paint().apply {
        isAntiAlias = true
        color = _targetSuccessColor
        style = Paint.Style.STROKE
        strokeWidth = _pointSize * 0.5f
    }
    val circleStrokeTargetShapeSuccess = Paint().apply {
        isAntiAlias = true
        color = _targetSuccessColor
        style = Paint.Style.STROKE
        strokeWidth = _pointSize * 0.2f
    }
    val fillPaintTargetShapeSuccess = Paint().apply {
        isAntiAlias = true
        color = backgroundColor
        style = Paint.Style.FILL
    }

    val textPaintPlayerNameFill = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        textSize = 45f
        color = _textPaintPlayerName
        typeface = _typeface
    }

    val textPaintPlayerNameStroke = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        textSize = 45f
        color = backgroundColor
        style = Paint.Style.STROKE
        strokeWidth = 16f
        typeface = _typeface
    }
}