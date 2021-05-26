package de.htw.gezumi.model

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import de.htw.gezumi.calculation.Conversions
import de.htw.gezumi.filter.Filter
import de.htw.gezumi.filter.MedianFilter

class Device(val address: String, private val _txPower: Int, val bluetoothDevice: BluetoothDevice) {

    private val _name = MutableLiveData("")
    val name: LiveData<String> get() = _name

    private val _distance = MutableLiveData(0.0)
    val distance: LiveData<Double> get() = _distance

    private val _filter: Filter = MedianFilter()

    val rssiHistory = mutableListOf<Int>()

    fun setName(name: String) {
        // postValue makes it possible to post from other threads
        _name.postValue(name)
    }

    // convenience function
    fun addRssi(rssi: Int) {
        rssiHistory.add(rssi)
        val curDist = Conversions.rssiToDistance(rssi.toDouble(), _txPower)
        _distance.postValue(_filter.applyFilter(curDist))
        // TODO we don't know how often the device is discovered by the scan, so it might be good to limit the execution of the distance calculation
    }
}