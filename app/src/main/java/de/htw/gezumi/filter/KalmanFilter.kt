package de.htw.gezumi.filter

/**
 * Credits: created by fgroch on 29.08.16.
 * Taken from https://github.com/fgroch/beacon-rssi-resolver/blob/master/src/main/java/tools/blocks/filter/KalmanFilter.java
 */
class KalmanFilter {
    private var processNoise //Process noise
            : Double
    private var measurementNoise //Measurement noise
            : Double
    private var estimatedRSSI //calculated rssi
            = 0.0
    private var errorCovarianceRSSI //calculated covariance
            = 0.0
    private var isInitialized = false //initialization flag

    constructor() {
        processNoise = 0.125
        measurementNoise = 0.8
    }

    constructor(processNoise: Double, measurementNoise: Double) {
        this.processNoise = processNoise
        this.measurementNoise = measurementNoise
    }

    fun applyFilter(rssi: Double): Double {
        val priorRSSI: Double
        val kalmanGain: Double
        val priorErrorCovarianceRSSI: Double
        if (!isInitialized) {
            priorRSSI = rssi
            priorErrorCovarianceRSSI = 1.0
            isInitialized = true
        } else {
            priorRSSI = estimatedRSSI
            priorErrorCovarianceRSSI = errorCovarianceRSSI + processNoise
        }
        kalmanGain = priorErrorCovarianceRSSI / (priorErrorCovarianceRSSI + measurementNoise)
        estimatedRSSI = priorRSSI + kalmanGain * (rssi - priorRSSI)
        errorCovarianceRSSI = (1 - kalmanGain) * priorErrorCovarianceRSSI
        return estimatedRSSI
    }
}