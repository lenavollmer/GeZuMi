package de.htw.gezumi.model

import java.nio.ByteBuffer

/**
 * Data container for sending via ble. Holds up to three float values associated with a device.
 */
class DeviceData(val deviceAddress: ByteArray, val values: FloatArray) {

    init {
        require(values.size <= 3) { "Too much data for one packet" }
    }

    fun toByteArray(): ByteArray {
        val byteBuffer = ByteBuffer.allocate(4 * values.size)
        values.forEach { byteBuffer.putFloat(it) }
        return deviceAddress + byteBuffer.array()
    }

    companion object {
        fun fromBytes(bytes: ByteArray): DeviceData {
            val deviceAddress = bytes.sliceArray(0 until 5)
            val values: MutableList<Float> = mutableListOf()
            val byteBuffer = ByteBuffer.wrap(bytes.sliceArray(5 until bytes.size))
            while(byteBuffer.hasRemaining()) {
                values.add(byteBuffer.float)
            }
            return DeviceData(deviceAddress, values.toFloatArray())
        }
    }
}