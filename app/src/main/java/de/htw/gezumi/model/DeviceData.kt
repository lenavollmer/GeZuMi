package de.htw.gezumi.model

import java.nio.ByteBuffer

class DeviceData(val deviceAddress: String, val value: Float) {

    companion object {

        fun fromBytes(bytes: ByteArray): DeviceData {
            val deviceAddress = toHexString(bytes.slice(0 until 6).toByteArray())
            val value = ByteBuffer.wrap(bytes.slice(6 until bytes.size).toByteArray()).float
            return DeviceData(deviceAddress, value)
        }

        private fun toHexString(bytes: ByteArray) = bytes.asUByteArray().joinToString(":") { it.toString(16).padStart(2, '0') }
    }

    fun toByteArray(): ByteArray {
        // newline \n = 0x0A
        val hexString = deviceAddress.replace(":", "")
        return decodeHex(hexString) + ByteBuffer.allocate(4).putFloat(value).array()
    }

    private fun decodeHex(hexString: String): ByteArray {
        require(hexString.length % 2 == 0) { "Must have an even length" }
        return hexString.chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
    }
}