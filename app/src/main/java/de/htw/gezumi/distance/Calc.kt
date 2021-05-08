package de.htw.gezumi.distance

import kotlin.math.pow

class Calc {
    companion object {

        /**
         * Calculates the distance for the given [rssi] (BLE signal strength).
         * [txPower] is the RSSI value with which the distance is 1 meter.
         * @return the distance in meters
         */
        fun rssiToDistance(rssi: Double, txPower: Double): Double {
            if (rssi == 0.0) {
                return -1.0
            }
            val ratio = rssi * 1.0 / txPower
            return if (ratio < 1.0) {
                ratio.pow(10.0)
            } else {
                (0.89976) * ratio.pow(7.7095) + 0.111
            }
        }
    }
}