package de.htw.gezumi.canvas

import android.graphics.Canvas
import android.graphics.Paint
import de.htw.gezumi.calculation.Vec
import de.htw.gezumi.model.Player

class Painter(private val _paints: Paints, private val _pointSize: Float) {

    fun drawWinningFigure(canvas: Canvas, positions: List<Vec>) {
        drawFigure(
            canvas,
            positions,
            _paints.lineStrokeTargetShapeSuccess,
            _paints.circleStrokeTargetShapeSuccess,
            _paints.fillPaintTargetShapeSuccess,
            _pointSize * 1.2f
        )
    }

    fun drawFigure(
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

    fun drawPlayerFigure(
        canvas: Canvas, players: List<Vec>,
    ) {
        drawFigure(
            canvas,
            players,
            _paints.lineStroke,
            _paints.circleStroke,
            _paints.fillPaint,
            _pointSize
        )
    }

    fun drawTargetShape(
        canvas: Canvas, targets: List<Vec>,
    ) {
        drawFigure(
            canvas,
            targets,
            _paints.lineStrokeTargetShape,
            _paints.circleStrokeTargetShape,
            _paints.fillPaintTargetShape,
            _pointSize * 1.2f
        )
    }

    fun drawPlayerNames(
        canvas: Canvas,
        players: List<Player>,
        playerScaledPositions: List<Vec>,
    ) {
        playerScaledPositions.forEachIndexed { i, position ->
            val y = position.y - _pointSize - 20f
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

    fun drawPlayerSelf(
        canvas: Canvas,
        playerSelfPosition: Vec,
    ) {
        canvas.drawCircle(playerSelfPosition.x, playerSelfPosition.y, _pointSize, _paints.playerSelfStroke)
    }

    fun clearCanvas(canvas: Canvas) {
        canvas.drawColor(_paints.backgroundColor)
    }
}
