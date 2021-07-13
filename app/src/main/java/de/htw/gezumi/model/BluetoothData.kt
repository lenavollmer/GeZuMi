package de.htw.gezumi.model

import de.htw.gezumi.viewmodel.DEVICE_ID_LENGTH
import java.nio.ByteBuffer

/**
 * Data container for sending via ble. Holds the sender id and up to two float values associated with a device.
 * Max. size: 20 bytes
 */
class BluetoothData(val id: ByteArray, val senderId: ByteArray, val values: FloatArray) {

    init {
        require(values.size <= 2) { "Too much data for one package" }
    }

    fun toByteArray(): ByteArray {
        val byteBuffer = ByteBuffer.allocate(4 * values.size)
        values.forEach { byteBuffer.putFloat(it) }
        return id + senderId + byteBuffer.array()
    }

    companion object {
        fun fromBytes(bytes: ByteArray): BluetoothData {
            val deviceId = bytes.sliceArray(0 until DEVICE_ID_LENGTH)
            val senderId = bytes.sliceArray(DEVICE_ID_LENGTH until DEVICE_ID_LENGTH * 2)
            val values: MutableList<Float> = mutableListOf()
            val byteBuffer = ByteBuffer.wrap(bytes.sliceArray(DEVICE_ID_LENGTH * 2 until bytes.size))
            while(byteBuffer.hasRemaining()) {
                values.add(byteBuffer.float)
            }
            return BluetoothData(deviceId, senderId, values.toFloatArray())
        }

        fun fromDevice(device: Device, senderId: ByteArray): BluetoothData = BluetoothData(device.deviceId, senderId, floatArrayOf(device.distance.value!!))
    }
}