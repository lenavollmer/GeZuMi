package de.htw.gezumi.model

import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import de.htw.gezumi.calculation.Conversions
import de.htw.gezumi.filter.Filter
import de.htw.gezumi.filter.MedianFilter

private const val TAG = "Device"

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
        // TODO get real txPower values, 127 means there is no txPower value
        val unfilteredDistance = Conversions.rssiToDistance(rssi.toDouble(), if(_txPower != 127) _txPower else 70)
        _distance.postValue(_filter.applyFilter(unfilteredDistance))
        Log.d(TAG, "unfilteredDistance: $unfilteredDistance, rssi: $rssi, txPower: $_txPower")
        // TODO we don't know how often the device is discovered by the scan, so it might be good to limit the execution of the distance calculation
    }

    fun getDeviceData(): DeviceData {
        return de.htw.gezumi.model.DeviceData(
            deviceId,
            floatArrayOf(_distance.value!!.toFloat()/* add up to 2 more values here*/)
        )
    }

    override fun equals(other: Any?): Boolean {
        return deviceId contentEquals (other as Device).deviceId
    }

}