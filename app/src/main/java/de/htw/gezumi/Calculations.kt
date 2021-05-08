package de.htw.gezumi

import de.htw.gezumi.filter.KalmanFilter
import de.htw.gezumi.filter.MedianFilter
import de.htw.gezumi.model.Device
import kotlin.math.pow

private const val TAG = "Calculations"

class Calculations {
    companion object {

        /**
         * Calculates the distance for the given [rssi] (BLE signal strength).
         * [txPower] is the RSSI value with which the distance is 1 meter.
         * @return the distance in meters
         */
        fun calculateDistance(rssi: Double, txPower: Int): Double {
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