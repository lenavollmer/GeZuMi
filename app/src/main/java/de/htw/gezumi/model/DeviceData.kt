package de.htw.gezumi.model

import java.io.*
import java.nio.ByteBuffer

class DeviceData(val deviceAddress: String, val value: Float): Serializable {

    companion object {
        const val separator = 0x0A.toByte()

        fun fromBytes(bytes: ByteArray): DeviceData {
            val separatorIndex = indexOf(bytes, separator)
            val deviceAddress = bytes.slice(0 until separatorIndex).toByteArray().toString(Charsets.UTF_8)
            val value = ByteBuffer.wrap(bytes.slice(separatorIndex + 2 until bytes.size).toByteArray()).float
            return DeviceData(deviceAddress, value)
        }

        private fun indexOf(searchArray: ByteArray, toBeFound: Byte): Int {
            for (i in searchArray.indices) {
                if (searchArray[i] == toBeFound)
                    return i;
            }
            return -1;
        }

    }

    fun toByteArray(): ByteArray {
        /*val bos = ByteArrayOutputStream()
        val out = ObjectOutputStream(bos)
        out.writeObject(this)
        out.flush()
        val bytes = bos.toByteArray()
        out.close()
        bos.close()
        return bytes*/
        // newline \n = 0x0A
        return deviceAddress.toByteArray(Charsets.UTF_8) + separator + ByteBuffer.allocate(4).putFloat(value).array()
    }
}