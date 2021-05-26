package de.htw.gezumi.calculation

import android.graphics.Point
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.abs
import kotlin.math.sqrt


class Conversions {
    companion object {

        /**
         * Calculates the distance for the given [rssi] (BLE signal strength).
         * [txPower] is the RSSI value with which the distance is 1 meter.
         * @return the distance in meters
         */
        fun rssiToDistance(rssi: Double, txPower: Int): Double {
            val envFactor = 3.0
            return 10.0.pow((txPower.toDouble() - rssi) / (10.0 * envFactor))
        }

        /**
         * Calculates the positions for the given [distances] matrix.
         * @return the calculated positions
         */
        fun distancesToPoints(distances: List<List<Double>>): List<Vec> {
            val points = mutableListOf<Vec>()

            // assume that the first point is a fix point
            points.add(Vec(0, 0))

            // compute position of the second point with the following assumptions:
            // 1. it shares the same y coordinate as the first point
            // 2. it is to the right of the first point
            points.add(Vec((points[0].x - distances[0][1]), points[0].y));

            // compute positions of all other points
            for (i in distances.indices.drop(2)) {
                val a = distances[0][1]
                val b = distances[0][i]
                val c = distances[1][i]
                val possiblePoints = getRelativePos(a, b, c, points[0])
                println(possiblePoints)

                // take the point that matches more closely to the already calculated points and their distances
                var favor1 = 0
                var favor2 = 0
                for (i2 in points.indices.drop(2)) {
                    val difference1 = getDist(possiblePoints.first, points[i2]) - distances[i2][i]
                    val difference2 = getDist(possiblePoints.second, points[i2]) - distances[i2][i]
                    if (abs(difference1) < abs(difference2)) favor1++
                    else favor2++
                }
                points.add(if (favor1 >= favor2) possiblePoints.first else possiblePoints.second)
            }
            return points
        }

        /**
         * Calculates the relative position to the [basePoint] using the given distances ([a],[b],[c]).
         * [a] is the distance between the [basePoint] and the point A that meets 2 conditions:
         *  1. it shares the same y coordinate as the [basePoint]
         *  2. it is to the right of the [basePoint]
         * [b] the distance between the point that needs be calculated and the [basePoint]
         * [c] the distance between the point that needs be calculated and A
         * @return pair of two possible points as the new point can be mirrored vertically
         */
        private fun getRelativePos(a: Double, b: Double, c: Double, basePoint: Vec): Pair<Vec, Vec> {
            val angle = acos((a.pow(2) + b.pow(2) - c.pow(2)) / (2 * a * b));
            val x = (basePoint.x - b * cos(angle))
            val yAnglePos = (b * sin(angle))
            return Pair(
                Vec(x, basePoint.y - yAnglePos), // point is below basePoint and A
                Vec(x, basePoint.y + yAnglePos)  // point is above basePoint and A
            )
        }

        /**
         * Get the euclidean distance between point [a] and point [b].
         */
        private fun getDist(a: Vec, b: Vec) = (a - b).length()

    }
}