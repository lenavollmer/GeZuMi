package de.htw.gezumi.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import de.htw.gezumi.calculation.Vec

class Player(val deviceId: ByteArray) {

    private val _name = MutableLiveData("")
    val name: LiveData<String> get() = _name

    var position: Vec? = null

    fun setName(name: String) {
        _name.postValue(name)
    }

    override fun toString(): String {
        return "Device ID: $deviceId | Position $position"
    }
}