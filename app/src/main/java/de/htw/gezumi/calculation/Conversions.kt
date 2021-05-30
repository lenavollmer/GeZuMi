package de.htw.gezumi.calculation

import android.util.Log
import kotlin.math.acos
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.abs


class Conversions {
    companion object {

        /**
         * Calculates the distance for the given [rssi] (BLE signal strength).
         * [txPower] is the RSSI value with which the distance is 1 meter.
         * @return the distance in meters
         */
        fun rssiToDistance(rssi: Float, txPower: Int): Float {
            val envFactor = 3f
            val distance = 10f.pow((-txPower.toFloat() - rssi) / (10f * envFactor))
            Log.d("Distance Calculation", "unfilteredDistance: $distance, rssi: $rssi, txPower: $txPower")
            return distance
        }

        /**
         * Calculates the positions for the given [distances] matrix.
         * @return the calculated positions, indices are preserved
         */
        fun distancesToPoints(distances: Array<FloatArray>): List<Vec> {
            val dists = fixDistanceMat(distances)
            val points = mutableListOf<Vec>()

            // assume that the first point is a fix point
            points.add(Vec(0, 0))

            // compute position of the second point with the following assumptions:
            // 1. it shares the same y coordinate as the first point
            // 2. it is to the right of the first point
            points.add(Vec((points[0].x - dists[0][1]), points[0].y));

            // compute positions of all other points
            for (i in dists.indices.drop(2)) {
                val a = dists[0][1]
                val b = dists[0][i]
                val c = dists[1][i]
                val possiblePoints = getRelativePos(a, b, c, points[0])
                println(possiblePoints)

                // take the point that matches more closely to the already calculated points and their distances
                var favor1 = 0
                var favor2 = 0
                for (i2 in points.indices.drop(2)) {
                    val difference1 = getDist(possiblePoints.first, points[i2]) - dists[i2][i]
                    val difference2 = getDist(possiblePoints.second, points[i2]) - dists[i2][i]
                    if (abs(difference1) < abs(difference2)) favor1++
                    else favor2++
                }
                points.add(if (favor1 >= favor2) possiblePoints.first else possiblePoints.second)
            }
            return points
        }

        /**
         * Make the distance matrix valid and symmetric.
         */
        private fun fixDistanceMat(distances: Array<FloatArray>): Array<FloatArray> {
            val dists = distances.copyOf()

            for (row in distances.indices) {
                for (col in distances[0].indices.drop(row + 1)) {
                    val first = distances[row][col]
                    val second = distances[col][row]
                    val distance = if (first != 0f && second != 0f) (first + second) / 2
                    else if (first == 0f && second != 0f) second
                    else if (first != 0f && second == 0f) first
                    else 0f
                    dists[row][col] = distance
                    dists[col][row] = distance
                }
            }
            return dists
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
        private fun getRelativePos(a: Float, b: Float, c: Float, basePoint: Vec): Pair<Vec, Vec> {
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