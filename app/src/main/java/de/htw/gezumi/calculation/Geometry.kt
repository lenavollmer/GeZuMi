package de.htw.gezumi.calculation

import android.graphics.Point

class Geometry {
    companion object {
        /**
         * Scales the given [points] to a canvas with the given [height] and [width].
         *  @return the scaled and centered points
         */
        fun scaleToCanvas(points: List<Point>, height: Int, width: Int, margin: Int): List<Point> {
            // move points to top left position
            val h = height - margin * 2
            val w = width - margin * 2
            val minX = points.map { it.x }.minOrNull() ?: 0
            val minY = points.map { it.y }.minOrNull() ?: 0
            var newPoints = points.map { Point(it.x - minX, it.y - minY) }

            // scale points as much as possible
            var maxX = newPoints.map { it.x }.maxOrNull() ?: 0
            var maxY = newPoints.map { it.y }.maxOrNull() ?: 0
            val yScaleFactor = h / maxY.toDouble()
            val xScaleFactor = w / maxX.toDouble()

            val scaleFactor = if (xScaleFactor < yScaleFactor) xScaleFactor else yScaleFactor;
            newPoints = newPoints.map { Point((it.x * scaleFactor).toInt(), (it.y * scaleFactor).toInt()) }

            // center everything
            if (xScaleFactor < yScaleFactor) {
                maxY = newPoints.map { it.y }.maxOrNull() ?: 0
                return newPoints.map { Point(it.x + margin, it.y + (h - maxY) / 2 + margin) }
            }
            maxX = newPoints.map { it.x }.maxOrNull() ?: 0
            return newPoints.map { Point(it.x + (w - maxX) / 2 + margin, it.y + margin) }
        }
    }
}