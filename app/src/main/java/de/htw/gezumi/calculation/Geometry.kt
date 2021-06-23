package de.htw.gezumi.calculation

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin


class Geometry {
    companion object {
        /**
         * Scales the given [points] to a canvas with the given [height] and [width].
         *  @return the scaled and centered points
         */
        private fun scaleToCanvas(points: List<Vec>, height: Int, width: Int, margin: Int): List<Vec> {
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
        private fun getAngleSigned(u: Vec, v: Vec): Float {
            val dot = u.dot(v)
            val det = u.x * v.y - u.y * v.x
            return atan2(det, dot)
        }

        /**
         * [points] are three positions. The first one is the angle's origin and the other two span the angle.
         * Returns the points that is further clock wise.
         */
        private fun getClockwisePoint(points: List<Vec>): Vec {
            val angle = getAngleSigned(points[1] - points[0], points[2] - points[0])
            val index = if (angle < 0) 0 else 1
            return points[1 + index]
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
        private fun rotatePoints(points: List<Vec>, o: Vec, angle: Float): List<Vec> =
            points.map { rotatePoint(it, o, angle) }


        fun determineMatch(players: List<Vec>, targets: List<Vec>): Boolean {
            val tolerance = 1 // the max distance in meter between the points to be a match
            val foundPoints = players.map { a ->
                // TODO two or more player points could match to the same target point
                targets.find { b ->
                    (b - a).length() <= tolerance
                }
            }
            return foundPoints.filterNotNull().size == players.size
        }

        /**
         * Generates a geometric object (e.g. triangle) with the given number of [edges].
         */
        fun generateGeometricObject(edges: Int): List<Vec> {
            val generatedPoints = mutableListOf<Vec>()
            repeat(edges) {
                var newPoint = Vec((0..250).random() / 100f, (0..300).random() / 100f)
                // make sure the new point is at least one meter away from all other already generated points
                while (generatedPoints.any { it.getDist(newPoint) < 1f }) {
                    newPoint = Vec((0..250).random() / 100f, (0..300).random() / 100f)
                }
                generatedPoints.add(newPoint)
            }
            return generatedPoints
        }

        /**
         * Arranges [playerPositions] and [targetPositions] on a canvas with the given [height] and [width].
         */
        fun arrangeGamePositions(
            playerPositions: List<Vec>,
            targetPositions: List<Vec>,
        ): GamePositions {
            // use closest point to host of target shape as base point of the target shape
            val hostPosition = playerPositions[0]
            var targets = targetPositions.toList();

            // translate player positions to target shape
            var players = playerPositions.map { it + targets[0] - hostPosition }
            val base = players[0]

            // rotate player positions to fit target shape
            val playersRightPoint = getClockwisePoint(players)
            val targetRightPoint = getClockwisePoint(targets)

            val angleToRotate = getAngleSigned(
                playersRightPoint - base,
                targetRightPoint - base
            )
            players = rotatePoints(
                players, base, angleToRotate
            )

            // center players and target shape independently
            players = center(players)
            targets = center(targets)

            return GamePositions(players, targets)
        }

        /**
         * Scale [scaleGamePositions] to a canvas with the given dimension.
         */
        fun scaleGamePositions(
            gamePositions: GamePositions,
            height: Int,
            width: Int,
            margin: Int
        ): GamePositions {
            val allPoints = scaleToCanvas(
                gamePositions.players + gamePositions.targets,
                height,
                width,
                margin
            )
            return GamePositions(
                allPoints.subList(0, gamePositions.players.size),
                allPoints.subList(gamePositions.players.size, allPoints.size),
            )
        }
    }
}

data class GamePositions(
    val players: List<Vec>,
    val targets: List<Vec>,
)
