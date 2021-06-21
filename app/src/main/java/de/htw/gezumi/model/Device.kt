package de.htw.gezumi.model

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import de.htw.gezumi.Utils
import de.htw.gezumi.calculation.Conversions
import de.htw.gezumi.filter.Filter
import de.htw.gezumi.filter.KalmanFilter
import de.htw.gezumi.viewmodel.GameViewModel
import java.io.File
import java.io.FileOutputStream


private const val TAG = "Device"

/**
 * [txPower] is a device specific value which allows to calculate a device agnostic rssi value (attenuation).
 */
class Device(val deviceId: ByteArray, var txPower: Short, var bluetoothDevice: BluetoothDevice?) { // bluetoothDevice changes unfortunately

    init {
        Thread {
            while (GameViewModel.instance.devices.contains(this)) {
                Thread.sleep(3000)
                writeRSSILog()
            }
        }.start()
    }

    val gameName = MutableLiveData("")

    private val _distance = MutableLiveData(0f)
    val distance: LiveData<Float> get() = _distance

    private val _filter: Filter = KalmanFilter()

    var lastSeen: Long = System.currentTimeMillis()

    val rssiHistory = mutableListOf<Int>()
    val distanceHistory = mutableListOf<Float>()

    @kotlin.ExperimentalUnsignedTypes
    fun addRssi(rssi: Int) {
        rssiHistory.add(rssi)
        Log.d(TAG, "Adding RSSI for device: ${Utils.logDeviceId(deviceId)}")
        val unfilteredDistance = Conversions.rssiToDistance(rssi.toFloat(), txPower)
        val distanceFloat = _filter.applyFilter(unfilteredDistance)
        distanceHistory.add(distanceFloat)
        _distance.postValue(_filter.applyFilter(unfilteredDistance))
        Log.d(TAG, "MEASM: rssi: $rssi,  distance: ${_distance.value}")
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

    @kotlin.ExperimentalUnsignedTypes
    private fun writeRSSILog() {
        val root: String = GameViewModel.instance.getApplication<Application>().applicationContext.getExternalFilesDir(null).toString();
        //val root: String = Environment.getExternalStorageDirectory().toString()
        val myDir = File("$root/gezumi_data")
        if (!myDir.exists()) {
            myDir.mkdirs()
        }
        val fname = "${Utils.toHexString(deviceId)}_${txPower}_distance_log.txt"
        val file = File(myDir, fname)
        if (file.exists()) file.delete()
        try {
            val out = FileOutputStream(file)
            out.write(makeLogString().toByteArray(Charsets.UTF_8))
            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
/*
        FileStorage.writeFile(
            GameViewModel.instance.getApplication<Application>().applicationContext,
            "${deviceId}_${txPower}_distance_log.txt",
            makeLogString()
        )*/
    }

    private fun makeLogString(): String {
        var str: String = "\"rssi\"; \"distance\"\n"
        for (i in rssiHistory.indices) {
            str += "${rssiHistory[i]};${distanceHistory[i]}\n"
        }
        return str
    }

}