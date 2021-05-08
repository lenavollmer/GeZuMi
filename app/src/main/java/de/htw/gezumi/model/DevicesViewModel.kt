package de.htw.gezumi.model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.htw.gezumi.Calculations
import de.htw.gezumi.filter.KalmanFilter
import de.htw.gezumi.filter.MedianFilter
import kotlin.concurrent.thread
import kotlin.math.pow

private const val TAG = "DevicesViewModel"

class DevicesViewModel : ViewModel() {

    private val _devices = mutableListOf<Device>()
    val devices: List<Device> get() = _devices

    lateinit var host: Device

    private val _medianValues = mutableMapOf<Device, ArrayList<Double>>()
    private val _kalmanValues = mutableMapOf<Device, ArrayList<Double>>()

    init {
        // TODO add pause and stop logic + make this a coroutine
        thread {
            while(true) {
                for(device in devices) {
                    if (device.rssiHistory.isEmpty()) continue
                    _kalmanValues[device]!!.add(Calculations.applyKalman(device))
                    _medianValues[device]!!.add(Calculations.applyMedian(device))
                    device.setDistance(Calculations.calculateDistance(_medianValues[device]!!.last()))
                }
            }
        }
    }

    fun addDevice(device: Device) {
        _devices.add(device)
        _medianValues[device] = ArrayList()
        _kalmanValues[device] = ArrayList()
    }

    /**
     * Only for debugging/testing purpose.
     * Log a csv string which can be used to visualize the measured distances.
     */
    fun logVisualizationCSV(device: Device) {
        var csvString = "\"Time\";\"Real\";\"Kalman\";\"Median\"\n"
        for ((i, kalman) in _kalmanValues[device]!!.withIndex()) {
            val time = i.toDouble() / 2
            csvString += "${time};" +
                    "${Calculations.calculateDistance(device.rssiHistory[i])};" +
                    "${Calculations.calculateDistance(kalman)};" +
                    "${Calculations.calculateDistance(_medianValues[device]!![i])}\n"
        }
        Log.d(TAG, csvString);
    }
}