package de.htw.gezumi.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class Device(val address: String) {

    private val _name = MutableLiveData("")
    val name: LiveData<String> get() = _name

    private val _distance = MutableLiveData(0.0)
    val distance: LiveData<Double> get() = _distance

    val rssiHistory = mutableListOf<Double>()

    fun setName(name: String) {
        // postValue makes it possible to post from other threads
        _name.postValue(name)
    }

    fun setDistance(distance: Double) = _distance.postValue(distance)

    // convenience function
    fun addRssi(rssi: Int) = rssiHistory.add(rssi.toDouble())

    // convenience function
    fun getLastRssiValue(): Double = rssiHistory.last()

}