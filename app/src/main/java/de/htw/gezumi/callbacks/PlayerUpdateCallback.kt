package de.htw.gezumi.callbacks

import de.htw.gezumi.model.DeviceData

interface PlayerUpdateCallback {

    fun onPlayerUpdate(deviceData: DeviceData)
}