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

        _distance.postValue(calculateRSSI(getMedian()))
    }
    fun clearList(){
        _values.clear()
    }

    // Discuss: How often should we call this?
    // Really every time we get a new value? Maybe only every 10/... ms?
    // How frequently do we want to remove values from the List?
    private fun getMedian(): Double {
        // Remove 'oldest' value after one minute
        // Make sure that this doesn't loop!
        while(_values.size > 61) _values.remove(1)

        val sortedArray = _values.sorted()

        return if (sortedArray.size % 2 === 0) (sortedArray[sortedArray.size / 2].toDouble() + sortedArray[
                sortedArray.size / 2 - 1].toDouble()) / 2
        else sortedArray[sortedArray.size / 2].toDouble()
    }

    private fun calculateRSSI(rssi: Double): Double {
        val txPower = -59 // hard-coded power value. Usually ranges between -59 to -65
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