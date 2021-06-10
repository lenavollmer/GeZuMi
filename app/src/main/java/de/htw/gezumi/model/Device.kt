package de.htw.gezumi.model

import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import de.htw.gezumi.Utils
import de.htw.gezumi.calculation.Conversions
import de.htw.gezumi.filter.Filter
import de.htw.gezumi.filter.MedianFilter

private const val TAG = "Device"

/**
 * [txPower] is a device specific value which allows to calculate a device agnostic rssi value (attenuation).
 */
class Device(val deviceId: ByteArray, var txPower: Short, var bluetoothDevice: BluetoothDevice?) { // bluetoothDevice changes unfortunately

    val gameName = MutableLiveData("")

    private val _distance = MutableLiveData(0f)
    val distance: LiveData<Float> get() = _distance

    private val _filter: Filter = MedianFilter()

    var lastSeen: Long = System.currentTimeMillis()

    val rssiHistory = mutableListOf<Int>()

    fun addRssi(rssi: Int) {
        rssiHistory.add(rssi)
        Log.d(TAG, "Adding RSSI for device: ${Utils.logDeviceId(deviceId)}")
        val unfilteredDistance = Conversions.rssiToDistance(rssi.toFloat(), txPower)
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