package de.htw.gezumi.filter

/**
 * Credits: created by fgroch on 29.08.16.
 * Taken from https://github.com/fgroch/beacon-rssi-resolver/blob/master/src/main/java/tools/blocks/filter/KalmanFilter.java
 */
class KalmanFilter: Filter {
    private var processNoise //Process noise
            : Float
    private var measurementNoise //Measurement noise
            : Float
    private var estimatedRSSI //calculated rssi
            = 0f
    private var errorCovarianceRSSI //calculated covariance
            = 0f
    private var isInitialized = false //initialization flag

    constructor() {
        processNoise = 0.125f
        measurementNoise = 0.8f
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