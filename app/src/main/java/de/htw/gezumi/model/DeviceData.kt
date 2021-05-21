package de.htw.gezumi.model

import android.annotation.SuppressLint
import de.htw.gezumi.Utils
import java.nio.ByteBuffer

class DeviceData(val deviceAddress: String, val value: Float) {
    @kotlin.ExperimentalUnsignedTypes
    @SuppressLint("DefaultLocale")
    companion object {

        fun fromBytes(bytes: ByteArray): DeviceData {
            val deviceAddress = toHexString(bytes.slice(0 until 6).toByteArray())
            val value = ByteBuffer.wrap(bytes.slice(6 until bytes.size).toByteArray()).float
            return DeviceData(deviceAddress, value)
        }
        private fun toHexString(bytes: ByteArray) = bytes.asUByteArray().joinToString(":") { it.toString(16).padStart(
            2,
            '0'
        )
        }.capitalize()
    }

    fun toByteArray(): ByteArray {
        val hexString = deviceAddress.replace(":", "")
        return Utils.decodeHex(hexString) + ByteBuffer.allocate(4).putFloat(value).array()
    }
}