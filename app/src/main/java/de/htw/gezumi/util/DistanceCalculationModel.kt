package de.htw.gezumi.util

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlin.math.pow

class DistanceCalculationModel : ViewModel() {

    private val _distance = MutableLiveData<Double>()
    val distance: LiveData<Double> = _distance

    private val _values = mutableListOf<Double>()

    fun addValue(value: Double){
        _values.add(value)
        _distance.value = calculateRSSI(value)
    }
    fun clearList(){
        _values.clear()
    }

    fun calculateRSSI(rssi: Double): Double {
        val txPower = -59 //hard coded power value. Usually ranges between -59 to -65
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