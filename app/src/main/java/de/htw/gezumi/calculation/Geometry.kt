package de.htw.gezumi.calculation

import android.graphics.Point
import kotlin.math.*


class Geometry {
    companion object {
        /**
         * Scales the given [points] to a canvas with the given [height] and [width].
         *  @return the scaled and centered points
         */
        fun scaleToCanvas(points: List<Vec>, height: Int, width: Int, margin: Int): List<Vec> {
            // move points to top left position
            var newPoints = moveTopLeft(points)

            // scale points as much as possible
            val h = height - margin * 2
            val w = width - margin * 2
            val maxX = newPoints.map { it.x }.maxOrNull() ?: 0
            val maxY = newPoints.map { it.y }.maxOrNull() ?: 0
            val yScaleFactor = h / maxY.toFloat()
            val xScaleFactor = w / maxX.toFloat()

            val scaleFactor = if (xScaleFactor < yScaleFactor) xScaleFactor else yScaleFactor
            newPoints = newPoints.map { Vec((it.x * scaleFactor), (it.y * scaleFactor)) }

            // center everything
            return center(newPoints, h, w).map { Vec(it.x + margin.toFloat(), it.y + margin.toFloat()) }
        }

        /**
         * Aligns [points] to the top left position.
         */
        private fun moveTopLeft(points: List<Vec>): List<Vec> {
            val minX = points.map { it.x }.minOrNull() ?: 0
            val minY = points.map { it.y }.minOrNull() ?: 0
            return points.map { Vec(it.x - minX.toFloat(), it.y - minY.toFloat()) }
        }

        /**
         * Centers [points] horizontally and vertically on a canvas with the given [height] and [width].
         */
        fun center(points: List<Vec>, height: Int = 1000, width: Int = 1000): List<Vec> {
            val translatedPoints = moveTopLeft(points)
            val maxX = translatedPoints.map { it.x }.maxOrNull() ?: 0
            val maxY = translatedPoints.map { it.y }.maxOrNull() ?: 0
            return translatedPoints.map {
                Vec(
                    it.x + (width - maxX.toFloat()) / 2,
                    it.y + (height - maxY.toFloat()) / 2
                )
            }
        }

        /**
         * Calculate the angle between two vectors ([u],[v]).
         * The angle is positive if it is the inner angle and negative if it is the outer angle.
         */
        fun getAngleSigned(u: Vec, v: Vec): Float {
            val dot = u.dot(v)
            val det = u.x * v.y - u.y * v.x
            return atan2(det, dot)
        }

        /**
         * The [points] are two position vectors that span an angle.
         * Returns the vector that comes later in clockwise direction and its index.
         */
        fun getClockwisePoint(points: Pair<Vec, Vec>): Pair<Vec, Int> {
            val angle = getAngleSigned(points.first, points.second)
            return if (angle < 0) Pair(points.first, 0) else Pair(points.second, 1)
        }

        /**
         * Rotates the [point] around the point [o] for the given [angle].
         */
        private fun rotatePoint(point: Vec, o: Vec, angle: Float): Vec {
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
        fun rotatePoints(points: List<Vec>, o: Vec, angle: Float): List<Vec> =
            points.map { rotatePoint(it, o, angle) }


        fun determineMatch(points: List<Vec>, targetShape: List<Vec>): Boolean {
            val tolerance = 0.0 // the max distance between the points to be a match
            val foundPoints = points.map { a ->
                // TODO two or more player points could match to the same target point
                targetShape.find { b ->
                    (b - a).length() <= tolerance
                }
            }
            return foundPoints.filterNotNull().size == points.size
        }

        /**
         * Generates a geometric object (e.g. triangle) with the given number of [edges].
         */
        fun generateGeometricObject(edges: Int): List<Point> {
            val generatedPoints = mutableListOf<Point>()
            for (i in 1..edges) {
                generatedPoints.add(Point((0..250).random(), (0..400).random()))
            }
            return generatedPoints
        }
    }
}