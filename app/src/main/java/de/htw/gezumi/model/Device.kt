package de.htw.gezumi.model

import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import de.htw.gezumi.calculation.Conversions
import de.htw.gezumi.filter.Filter
import de.htw.gezumi.filter.MedianFilter

private const val TAG = "Device"

class Device(val deviceId: ByteArray, var txPower: Int, var bluetoothDevice: BluetoothDevice?) { // bluetoothDevice changes unfortunately

    val gameName = MutableLiveData("")

    private val _distance = MutableLiveData(0f)
    val distance: LiveData<Float> get() = _distance

    private val _filter: Filter = MedianFilter()

    var lastSeen: Long = System.currentTimeMillis()

    val rssiHistory = mutableListOf<Int>()

    fun addRssi(rssi: Int) {
        rssiHistory.add(rssi)
        // TODO get real txPower values, 127 means there is no txPower value
        val unfilteredDistance = Conversions.rssiToDistance(rssi.toFloat(), if(txPower != 127) txPower else 70)
        _distance.postValue(_filter.applyFilter(unfilteredDistance))
        Log.d(TAG, "unfilteredDistance: $unfilteredDistance, rssi: $rssi, txPower: $txPower")
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