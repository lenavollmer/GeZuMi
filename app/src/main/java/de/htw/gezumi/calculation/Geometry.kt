package de.htw.gezumi.calculation

import android.R.attr
import android.graphics.Point
import kotlin.math.*


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

        /**
         * Calculate the angle between two vectors ([u],[v]).
         */
        fun getAngle(u: Point, v: Point): Double {
            val num: Int = v.x * u.x + v.y * u.y
            val den =
                sqrt(v.x.toDouble().pow(2.0) + v.y.toDouble().pow(2.0)) * sqrt(
                    u.x.toDouble().pow(2.0) + u.y.toDouble().pow(2.0)
                )
            return acos(num / den)
        }

        fun translateTo(points: List<Point>, origin: Point = Point(0, 0)): List<Point> {
            val xDiff = origin.x - points[0].x
            val yDiff = origin.y - points[0].y
            return points.map { Point(it.x + xDiff, it.y + yDiff) }
        }

        fun getRightPoint(points: List<Point>): Point {
            val centeredPoints = translateTo(points)
            val angle = getAngle(centeredPoints[1], centeredPoints[2])
            return if (angle > 180) points[1] else points[2]
        }


        /**
         * [p] point to rotate, [o] point ot rotate around
         */
        fun rotatePoint(p: Point, o: Point, angle: Double): Point {
            val s = sin(angle)
            val c = cos(angle)

            // translate point back to origin:
            p.x -= o.x
            p.y -= o.y

            // rotate point

            // rotate point
            val xnew = p.x * c - p.y * s
            val ynew = p.x * s + p.y * c

            // translate point back:

            // translate point back:
            p.x = (xnew + o.x).toInt()
            p.y = (ynew + o.y).toInt()
            return p
        }

        fun rotatePoints(points: List<Point>, o: Point, angle: Double): List<Point> =
            points.map { rotatePoint(it, o, angle) }
    }
}