package de.htw.gezumi.calculation

import android.graphics.Point
import android.util.Log
import kotlin.math.*

private const val TAG = "Geometry"

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
            newPoints =
                newPoints.map { Point((it.x * scaleFactor).toInt(), (it.y * scaleFactor).toInt()) }

            // center everything
            if (xScaleFactor < yScaleFactor) {
                maxY = newPoints.map { it.y }.maxOrNull() ?: 0
                return newPoints.map { Point(it.x + margin, it.y + (h - maxY) / 2 + margin) }
            }
            maxX = newPoints.map { it.x }.maxOrNull() ?: 0
            return newPoints.map { Point(it.x + (w - maxX) / 2 + margin, it.y + margin) }
        }

        fun centerShapes(points: List<Point>, height: Int, width: Int, margin: Int): List<Point> {
            val h = height - margin * 2
            val w = width - margin * 2

            var maxX = points.map { it.x }.maxOrNull() ?: 0
            var maxY = points.map { it.y }.maxOrNull() ?: 0

            val yScaleFactor = h / maxY.toDouble()
            val xScaleFactor = w / maxX.toDouble()

            // center everything
            if (xScaleFactor < yScaleFactor) {
                return points.map { Point(it.x + margin, it.y + (h - maxY) / 2 + margin) }
            }

            return points.map { Point(it.x + (w - maxX) / 2 + margin, it.y + margin) }
        }

        /**
         * Calculate the angle between two vectors ([u],[v]).
         */
        fun getAngle(u: Vec, v: Vec): Double {
            val num = u.dot(v)
            val den =
                sqrt(v.x.pow(2.0) + v.y.pow(2.0)) * sqrt(
                    u.x.pow(2.0) + u.y.pow(2.0)
                )
            return acos(num / den)
        }

        fun getAngleClockwise(u: Vec, v: Vec): Double {
            val dot = u.dot(v)
            val det = u.x * v.y - u.y * v.x
            Log.d(TAG, "angle clockwise ${atan2(det, dot)}")
            return atan2(det, dot)
        }

        /**
         * The [points] are two position vectors that span an angle.
         * Returns the vector that comes later in clockwise direction and its index.
         */
        fun getClockwisePoint(points: Pair<Vec, Vec>): Pair<Vec, Int> {
            val angle = getAngleClockwise(points.first, points.second)
            return if (angle < 0) Pair(points.first, 0) else Pair(points.second, 1)
        }

        /**
         * Rotates the [point] around the point [o] for the given [angle].
         */
        private fun rotatePoint(point: Vec, o: Vec, angle: Double): Vec {
            val s = sin(angle)
            val c = cos(angle)
            // translate point back to origin:
            val cp = point - o
            // rotate point
            val new = Vec(cp.x * c - cp.y * s, cp.x * s + cp.y * c)
            // translate point back:
            return new + o
        }

        /**
         * Rotates the [points] around the point [o] for the given [angle].
         */
        fun rotatePoints(points: List<Vec>, o: Vec, angle: Double): List<Vec> =
            points.map { rotatePoint(it, o, angle) }


        fun determineMatch(points: List<Vec>, targetShape: List<Vec>, playerCount: Int): Boolean {
            val buffer = 0

            val foundPoints = points.map {
                targetShape.find { vec ->
                    vec.minus(it).length() >= -buffer.toDouble() && vec.minus(
                        it
                    ).length() <= buffer.toDouble()
                }
            }

            return foundPoints.filterNotNull().size == playerCount
        }
    }
}