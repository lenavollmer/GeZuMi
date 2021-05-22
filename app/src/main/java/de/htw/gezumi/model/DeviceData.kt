package de.htw.gezumi.model

import android.annotation.SuppressLint
import de.htw.gezumi.Utils
import java.nio.ByteBuffer
import java.nio.file.Files.size

/**
 * Data container for sending via ble. Holds up to three float values associated with a device.
 */
class DeviceData(val deviceAddress: String, val values: FloatArray) {

    init {
        require(values.size <= 3) { "Too much data for one packet" }
    }

    fun toByteArray(): ByteArray {
        val hexString = deviceAddress.replace(":", "")
        val byteBuffer = ByteBuffer.allocate(4 * values.size)
        values.forEach { byteBuffer.putFloat(it) }
        return Utils.decodeHex(hexString) + byteBuffer.array()
    }

    @kotlin.ExperimentalUnsignedTypes
    @SuppressLint("DefaultLocale")
    companion object {
        fun fromBytes(bytes: ByteArray): DeviceData {
            val deviceAddress = Utils.toHexString(bytes.slice(0 until 6).toByteArray())
            val values: MutableList<Float> = mutableListOf()
            val byteBuffer = ByteBuffer.wrap(bytes.slice(6 until bytes.size).toByteArray())
            while(byteBuffer.hasRemaining()) {
                values.add(byteBuffer.float)
            }
            return DeviceData(deviceAddress, values.toFloatArray())
        }
    }
}