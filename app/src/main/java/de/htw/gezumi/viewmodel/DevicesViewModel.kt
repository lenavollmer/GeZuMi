package de.htw.gezumi.viewmodel

import androidx.lifecycle.ViewModel
import de.htw.gezumi.model.Device

private const val TAG = "DevicesViewModel"

class DevicesViewModel : ViewModel() {

    private val _devices = mutableListOf<Device>()
    val devices: List<Device> get() = _devices

    lateinit var host: Device

    /*init {
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
    }*/

    fun addDevice(device: Device) {
        _devices.add(device)
    }
}