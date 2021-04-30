package de.htw.gezumi.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlin.math.pow

class DeviceViewModel : ViewModel() {
    private val _name = MutableLiveData("Unknown")
    val name: LiveData<String> get() = _name

    private val _distance = MutableLiveData(0.0)
    val distance: LiveData<Double> get() = _distance

    private val _values = mutableListOf<Int>()

    fun setName(name: String) {
        // postValue makes it possible to post from other threads
        _name.postValue(name)
    }

    fun addRSSI(value: Int){
        _values.add(value)
        _distance.postValue(calculateRSSI(value.toDouble()))
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