package de.htw.gezumi.calculation

import android.util.Log
import kotlin.math.*


class Conversions {
    companion object {

        /**
         * Calculates the distance for the given [rssi] (BLE signal strength) for a device with the given [txPower].
         * [txPower] allows to calculate a device agnostic rssi value (attenuation). Attenuation is calculated as follows:
         * attenuation = txPower - rssi
         * After attenuating the rssi it is similar to the rssi of an Iphone.
         *
         * @return the distance in meters
         */
        fun rssiToDistance(rssi: Float, txPower: Short): Float {
            val envFactor = 3f
            val attenuation = txPower - rssi
            val distance = 10f.pow((-56 + attenuation) / (10f * envFactor))
            Log.d(
                "Distance Calculation",
                "unfilteredDistance: $distance, rssi: $rssi, attenuation: $attenuation, txPower: $txPower"
            )
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
            points.add(Vec((points[0].x - dists[0][1]), points[0].y))

            // compute positions of all other points
            for (i in dists.indices.drop(2)) {
                val a = dists[0][1]
                val b = dists[0][i]
                val c = dists[1][i]
                if (!isValidTriangle(a, b, c)) throw IllegalArgumentException("Invalid triangle")
                val possiblePoints = getRelativePos(a, b, c, points[0])

                // take the point that matches more closely to the already calculated points and their distances
                var favor1 = 0
                var favor2 = 0
                for (i2 in points.indices.drop(2)) {
                    val difference1 = possiblePoints.first.getDist(points[i2]) - dists[i2][i]
                    val difference2 = possiblePoints.second.getDist(points[i2]) - dists[i2][i]
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
            // One side can not be larger than the sum of the other two.
            // If that is the case we clip the side to the sum of the other two.
            val aValid = clipSide(a, b, c)
            val bValid = clipSide(b, aValid, c)
            val cValid = clipSide(c, aValid, bValid)

            val angle =
                acos(
                    clipToRange(
                        (aValid.pow(2) + bValid.pow(2) - cValid.pow(2)) / (2 * aValid * bValid),
                        0.99999f,
                        -0.99999f
                    )
                )

            val x = (basePoint.x - bValid * cos(angle))
            val yAnglePos = (bValid * sin(angle))
            return Pair(
                Vec(x, basePoint.y - yAnglePos), // point is below basePoint and A
                Vec(x, basePoint.y + yAnglePos)  // point is above basePoint and A
            )
        }

        private fun isValidTriangle(a: Float, b: Float, c: Float) = a < b + c && b < a + c && c < a + b

        private fun clipToRange(value: Float, max: Float, min: Float): Float {
            if (value > max) return max
            if (value < min) return min
            return value
        }

        private fun clipSide(a: Float, b: Float, c: Float) = if (a > (b + c)) b + c else a

    }
}