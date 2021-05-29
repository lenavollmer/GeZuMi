package de.htw.gezumi.model

import de.htw.gezumi.viewmodel.DEVICE_ID_LENGTH
import java.nio.ByteBuffer

/**
 * Data container for sending via ble. Holds up to two float values associated with a device.
 */
class DeviceData(val deviceId: ByteArray, val senderId: ByteArray, val values: FloatArray) {

    init {
        require(values.size <= 2) { "Too much data for one packet" }
    }

    fun toByteArray(): ByteArray {
        val byteBuffer = ByteBuffer.allocate(4 * values.size)
        values.forEach { byteBuffer.putFloat(it) }
        return deviceId + senderId + byteBuffer.array()
    }

    companion object {
        fun fromBytes(bytes: ByteArray): DeviceData {
            val deviceId = bytes.sliceArray(0 until DEVICE_ID_LENGTH)
            val senderId = bytes.sliceArray(DEVICE_ID_LENGTH until DEVICE_ID_LENGTH * 2)
            val values: MutableList<Float> = mutableListOf()
            val byteBuffer = ByteBuffer.wrap(bytes.sliceArray(DEVICE_ID_LENGTH * 2 until bytes.size))
            while(byteBuffer.hasRemaining()) {
                values.add(byteBuffer.float)
            }
            return DeviceData(deviceId, senderId, values.toFloatArray())
        }

        fun fromDevice(device: Device, senderId: ByteArray): DeviceData = DeviceData(device.deviceId, senderId, floatArrayOf(device.distance.value!!))
    }
}