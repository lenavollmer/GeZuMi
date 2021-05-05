package de.htw.gezumi.model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.htw.gezumi.filter.KalmanFilter
import de.htw.gezumi.filter.MedianFilter
import kotlin.math.pow

class DeviceViewModel : ViewModel() {
    private val _name = MutableLiveData("Unknown")
    val name: LiveData<String> get() = _name

    private val _distance = MutableLiveData(0.0)
    val distance: LiveData<Double> get() = _distance


    private val _medianValues = mutableListOf<Double>()
    private val _medianFilter = MedianFilter()

    private val _kalmanValues = mutableListOf<Double>()
    private val _kalmanFilter = KalmanFilter()

    private val _realValues = mutableListOf<Double>()


    fun setName(name: String) {
        // postValue makes it possible to post from other threads
        _name.postValue(name)
    }

    fun addRSSI(rssi: Int) {
        var kalmanValue = _kalmanFilter.applyFilter(rssi.toDouble())
        _kalmanValues.add(kalmanValue)
        var medianValue = _medianFilter.applyFilter(rssi.toDouble())
        _medianValues.add(medianValue)
        _distance.postValue(getDistanceFromRSSI(medianValue))
        _realValues.add(rssi.toDouble())
    }

    /**
     * Calculate the distance for the given RSSI.
     */
    private fun getDistanceFromRSSI(rssi: Double): Double {
        // txPower is the hard coded transmission power value of the sending device
        // it is the RSSI value with which the distance is 1 meter
        // val txPower = -59
        val txPower = -71 // Xiaomi A2 Lite
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

    /**
     * Only for debugging/testing purpose.
     * Log a csv string which can be used to visualize the measured distances.
     */
    fun logVisualizationCSV() {
        var csvString = "\"Time\";\"Real\";\"Kalman\";\"Median\"\n"
        Log.d("Length:", _kalmanValues.size.toString());
        for((i, kalman) in _kalmanValues.withIndex()){
            val time = i.toDouble()/2
            csvString += "${time};${getDistanceFromRSSI(_realValues[i])};${getDistanceFromRSSI(kalman)};${
                getDistanceFromRSSI(
                    _medianValues[i]
                )
            }\n"
        }
        Log.d("CSV Data", csvString);
    }
}