package de.htw.gezumi.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import de.htw.gezumi.model.Device
import de.htw.gezumi.util.FileStorage


private const val TAG = "DevicesViewModel"

class DevicesViewModel : ViewModel() {

    private val _devices = mutableListOf<Device>()
    val devices: List<Device> get() = _devices

    lateinit var host: Device

    fun addDevice(device: Device) {
        _devices.add(device)
    }

    fun writeRSSILog(context: Context?) {
        if (context == null) return
        FileStorage.writeFile(
            context,
            "${java.util.Calendar.getInstance().time}_distance_log.txt",
            devices[0].rssiHistory.toString()
        )
    }


}