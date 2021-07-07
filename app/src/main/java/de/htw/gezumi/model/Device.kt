package de.htw.gezumi.model

import android.bluetooth.BluetoothDevice
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import de.htw.gezumi.calculation.Conversions
import de.htw.gezumi.filter.Filter
import de.htw.gezumi.filter.MedianFilter

/**
 * [txPower] is a device specific value which allows to calculate a device agnostic rssi value (attenuation).
 */
class Device(val deviceId: ByteArray, var txPower: Short, var bluetoothDevice: BluetoothDevice?) { // bluetoothDevice changes unfortunately

    val gameName = MutableLiveData("")

    private val _distance = MutableLiveData(0f)
    val distance: LiveData<Float> get() = _distance

    private val _filter: Filter = MedianFilter()

    var lastSeen: Long = System.currentTimeMillis()

    private val rssiHistory = mutableListOf<Int>()

    @kotlin.ExperimentalUnsignedTypes
    fun addRssi(rssi: Int) {
        rssiHistory.add(rssi)
        val unfilteredDistance = Conversions.rssiToDistance(rssi.toFloat(), txPower)
        _distance.postValue(_filter.applyFilter(unfilteredDistance))
    }

    override fun equals(other: Any?): Boolean {
        return deviceId contentEquals (other as Device).deviceId
    }

    override fun hashCode(): Int {
        return deviceId.contentHashCode()
    }

}