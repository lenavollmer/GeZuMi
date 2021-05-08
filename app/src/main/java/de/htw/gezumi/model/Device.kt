package de.htw.gezumi.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import de.htw.gezumi.Calculations
import de.htw.gezumi.filter.Filter
import de.htw.gezumi.filter.MedianFilter

class Device(val address: String, private val _txPower: Int) {

    private val _name = MutableLiveData("")
    val name: LiveData<String> get() = _name

    private val _distance = MutableLiveData(0.0)
    val distance: LiveData<Double> get() = _distance

    private val _filter: Filter = MedianFilter()

    fun setName(name: String) {
        // postValue makes it possible to post from other threads
        _name.postValue(name)
    }

    // convenience function
    fun addRssi(rssi: Int) {
        val curDist = Calculations.calculateDistance(rssi.toDouble(), _txPower)
        _distance.postValue(_filter.applyFilter(curDist))
    }

}