package de.htw.gezumi.filter

/**
 * Credits: created by fgroch on 29.08.16.
 * Taken from https://github.com/fgroch/beacon-rssi-resolver/blob/master/src/main/java/tools/blocks/filter/KalmanFilter.java
 */
class KalmanFilter: Filter {
    /**
     * R models the process noise and describes how noisy
     * our system internally is. Or, in other words, how much
     * noise can we expect from the system itself? As our
     * system is constant we can set this to a (very) low value.
      */
    private var processNoise //Process noise
            : Float

    /**
     * Q resembles the measurement noise; how much noise is
     * caused by our measurements? As we expect that our
     * measurements will contain most of the noise, it makes
     * sense to set this parameter to a high number
     * (especially in comparison to the process noise)
     */
    private var measurementNoise //Measurement noise
            : Float
    private var estimatedRSSI //calculated rssi
            = 0f
    private var errorCovarianceRSSI //calculated covariance
            = 0f
    private var isInitialized = false //initialization flag

    constructor() {
        processNoise = 0.05f //0.125f
        measurementNoise = 1f //0.8f
    }

    constructor(processNoise: Float, measurementNoise: Float) {
        this.processNoise = processNoise
        this.measurementNoise = measurementNoise
    }

    override fun applyFilter(rssi: Float): Float {
        val priorRSSI: Float
        val kalmanGain: Float
        val priorErrorCovarianceRSSI: Float
        if (!isInitialized) {
            priorRSSI = rssi
            priorErrorCovarianceRSSI = 1f
            isInitialized = true
        } else {
            priorRSSI = estimatedRSSI
            priorErrorCovarianceRSSI = errorCovarianceRSSI + processNoise
        }
        kalmanGain = priorErrorCovarianceRSSI / (priorErrorCovarianceRSSI + measurementNoise)
        estimatedRSSI = priorRSSI + kalmanGain * (rssi - priorRSSI)
        errorCovarianceRSSI = (1f - kalmanGain) * priorErrorCovarianceRSSI
        return estimatedRSSI
    }
}