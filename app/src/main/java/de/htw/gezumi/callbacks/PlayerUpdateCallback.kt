package de.htw.gezumi.callbacks

import de.htw.gezumi.model.BluetoothData

interface PlayerUpdateCallback {

    fun onPlayerUpdate(bluetoothData: BluetoothData)
}