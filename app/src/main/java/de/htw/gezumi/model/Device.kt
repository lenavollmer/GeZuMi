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
import de.htw.gezumi.filter.MedianFilter
import de.htw.gezumi.viewmodel.GameViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*


private const val TAG = "Device"

/**
 * [txPower] is a device specific value which allows to calculate a device agnostic rssi value (attenuation).
 */
class Device(val deviceId: ByteArray, var txPower: Short, var bluetoothDevice: BluetoothDevice?, val test: Boolean = false) { // bluetoothDevice changes unfortunately

    init {
        if (!test) {
            Thread {
                while (GameViewModel.instance.devices.contains(this)) {
                    Thread.sleep(5000)
                    writeRssiLog()
                }
            }.start()
        }
    }

    val gameName = MutableLiveData("")

    private val _distance = MutableLiveData(0f)
    val distance: LiveData<Float> get() = _distance

    private val _filterKalman: Filter = KalmanFilter()
    private val _filterMedian: Filter = MedianFilter()

    var lastSeen: Long = System.currentTimeMillis()

    val timestamps = mutableListOf<String>()

    val rssiHistoryUnfiltered = mutableListOf<Int>()
    val distanceHistoryUnfiltered = mutableListOf<Float>()

    val rssiHistoryKalman = mutableListOf<Float>()
    val distanceHistoryKalman = mutableListOf<Float>()

    val rssiHistoryMedian = mutableListOf<Float>()
    val distanceHistoryMedian = mutableListOf<Float>()

    @kotlin.ExperimentalUnsignedTypes
    fun addRssi(rssi: Int) {
        val formatter = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val timeString = formatter.format(Calendar.getInstance().time)
        addRssi(rssi, timeString)
    }

    @kotlin.ExperimentalUnsignedTypes
    fun addRssi(rssi: Int, timeString: String) {
        if (!test) Log.d(TAG, "Adding RSSI for device: ${Utils.logDeviceId(deviceId)}")
        timestamps.add(timeString)

        rssiHistoryUnfiltered.add(rssi)
        val distanceUnfiltered = Conversions.rssiToDistance(rssi.toFloat(), txPower, test)
        distanceHistoryUnfiltered.add(distanceUnfiltered)

        val filteredKalman = _filterKalman.applyFilter(rssi.toFloat())
        rssiHistoryKalman.add(filteredKalman)
        val distanceKalman = Conversions.rssiToDistance(filteredKalman, txPower, test)
        distanceHistoryKalman.add(distanceKalman)

        val filteredMedian = _filterMedian.applyFilter(rssi.toFloat())
        rssiHistoryMedian.add(filteredMedian)
        val distanceMedian = Conversions.rssiToDistance(filteredMedian, txPower, test)
        distanceHistoryMedian.add(distanceMedian)

        if (!test) _distance.postValue(distanceKalman)
        if (!test) Log.d(TAG, "MEASM: unfiltered rssi: $rssi,  kalman distance: ${distanceKalman}, median distance: $distanceMedian")
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

    fun writeRssiLog(file: File) {
        if (file.exists()) file.delete()
        try {
            val out = FileOutputStream(file)
            out.write(makeLogString().toByteArray(Charsets.UTF_8))
            out.flush()
            out.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun writeRssiLog() {
        val fname = "device${GameViewModel.instance.devices.indexOf(this)}_${txPower}_log.txt"
        if (!test) Log.d(TAG, "MEASM: save log file: $fname")

        val root: String = GameViewModel.instance.getApplication<Application>().applicationContext.getExternalFilesDir(null).toString()
        //val root: String = Environment.getExternalStorageDirectory().toString()
        val myDir = File("$root/gezumi_data")
        if (!myDir.exists()) {
            myDir.mkdirs()
        }

        val file = File(myDir, fname)
        writeRssiLog(file)
    }

    private fun makeLogString(): String {
        var str: String = "time_ms;rssi_unfiltered;rssi_kalman;rssi_median;distance_unfiltered;distance_kalman;distance_median;\n"
        for (i in rssiHistoryUnfiltered.indices) {
            str += "${timestamps[i]};${rssiHistoryUnfiltered[i]};${rssiHistoryKalman[i]};${rssiHistoryMedian[i]};" +
                    "${distanceHistoryUnfiltered[i]};${distanceHistoryKalman[i]};${distanceHistoryMedian[i]};\n"
        }
        return str
    }

}