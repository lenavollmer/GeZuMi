package de.htw.gezumi.model

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import de.htw.gezumi.calculation.Conversions
import de.htw.gezumi.filter.Filter
import de.htw.gezumi.filter.MedianFilter
import de.htw.gezumi.viewmodel.GameViewModel

class Device(val deviceId: ByteArray, private val _txPower: Int, var bluetoothDevice: BluetoothDevice) { // bluetoothDevice changes unfortunately

    val gameName = MutableLiveData("")

    private val _distance = MutableLiveData(0f)
    val distance: LiveData<Float> get() = _distance

    private val _filter: Filter = MedianFilter()

    var lastSeen: Long = System.currentTimeMillis()

    val rssiHistory = mutableListOf<Int>()

    fun addRssi(rssi: Int) {
        rssiHistory.add(rssi)      
        val unfilteredDistance = Conversions.rssiToDistance(rssi.toFloat(), _txPower)
        _distance.postValue(_filter.applyFilter(unfilteredDistance))
        // TODO we don't know how often the device is discovered by the scan, so it might be good to limit the execution of the distance calculation
    }
    /*
    fun getDeviceData(): DeviceData {
        return DeviceData(
            deviceId,
            GameViewModel.instance.myDeviceId,
            floatArrayOf(_distance.value!!.toFloat()/* add up to 1 more values here*/)
        )
    }*/

    override fun equals(other: Any?): Boolean {
        return deviceId contentEquals (other as Device).deviceId
    }

}