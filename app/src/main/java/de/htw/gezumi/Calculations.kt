package de.htw.gezumi

import de.htw.gezumi.filter.KalmanFilter
import de.htw.gezumi.filter.MedianFilter
import de.htw.gezumi.model.Device
import kotlin.math.pow

private const val TAG = "Calculations"

class Calculations {
    companion object {
        private val _medianFilter = MedianFilter()
        private val _kalmanFilter = KalmanFilter()

        fun applyKalman(device: Device): Double {
            return _kalmanFilter.applyFilter(device.getLastRssiValue())
        }

        fun applyMedian(device: Device): Double {
            return _medianFilter.applyFilter(device.getLastRssiValue())
        }

        /**
         * Calculate the distance of a device using the median value. TODO: adapt if we have more than 1 rssi value
         */
        fun calculateDistance(median: Double): Double {
            // txPower is the hard coded transmission power value of the sending device
            // it is the RSSI value with which the distance is 1 meter
            // val txPower = -59
            val txPower = -71 // Xiaomi A2 Lite
            if (median == 0.0) {
                return -1.0
            }
            val ratio = median * 1.0 / txPower
            return if (ratio < 1.0) {
                ratio.pow(10.0)
            } else {
                (0.89976) * ratio.pow(7.7095) + 0.111
            }
        }
    }
}