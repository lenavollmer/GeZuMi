package de.htw.gezumi

import kotlin.math.pow


class Calculations {
    companion object {

        /**
         * Calculates the distance for the given [rssi] (BLE signal strength).
         * [txPower] is the RSSI value with which the distance is 1 meter.
         * @return the distance in meters
         */
        fun calculateDistance(rssi: Double, txPower: Int): Double {
            val envFactor = 3.0
            return 10.0.pow((txPower.toDouble() - rssi) / (10.0 * envFactor))
        }
    }
}