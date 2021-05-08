package de.htw.gezumi.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import de.htw.gezumi.model.Device

class GameViewModel() : ViewModel() {
    val hostDevice = MutableLiveData<Device>(Device())

    fun addHostRssi(rssi: Int) {
        hostDevice.value?.addRSSI(rssi)
        // postValue makes it possible to post from other threads
        hostDevice.postValue(hostDevice.value)
    }

    fun setHostName(name: String) {
        hostDevice.value?.name = name
    }
}