package de.htw.gezumi.model

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import de.htw.gezumi.Calculations
import de.htw.gezumi.filter.Filter
import de.htw.gezumi.filter.MedianFilter

class Device(val deviceId: ByteArray, private val _txPower: Int, var bluetoothDevice: BluetoothDevice) { // bluetoothDevice changes unfortunately

    private val _name = MutableLiveData("")
    val name: LiveData<String> get() = _name

    val gameName = MutableLiveData("")

    private val _distance = MutableLiveData(0.0)
    val distance: LiveData<Double> get() = _distance

    private val _filter: Filter = MedianFilter()

    val rssiHistory = mutableListOf<Int>()

    fun setName(name: String) {
        // postValue makes it possible to post from other threads
        _name.postValue(name)
    }

    fun addRssi(rssi: Int) {
        rssiHistory.add(rssi)
        val unfilteredDistance = Calculations.calculateDistance(rssi.toDouble(), _txPower)
        _distance.postValue(_filter.applyFilter(unfilteredDistance))
        // TODO we don't know how often the device is discovered by the scan, so it might be good to limit the execution of the distance calculation
    }

    fun getDeviceData(): DeviceData {
        return DeviceData(deviceId, floatArrayOf(_distance.value!!.toFloat()/* add up to 2 more values here*/))
    }

    override fun equals(other: Any?): Boolean {
        return deviceId contentEquals (other as Device).deviceId
    }

}