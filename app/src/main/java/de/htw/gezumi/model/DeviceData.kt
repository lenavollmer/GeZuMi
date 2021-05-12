package de.htw.gezumi.model

import java.io.Serializable
import java.nio.ByteBuffer

class DeviceData(val deviceAddress: String, val value: Float): Serializable {

    companion object {
        const val separator = 0x0A.toByte()

        fun fromBytes(bytes: ByteArray): DeviceData {
            val deviceAddress = bytes.slice(0 until 6).toByteArray().toHexString()
            val value = ByteBuffer.wrap(bytes.slice(6 until bytes.size).toByteArray()).float
            return DeviceData(deviceAddress, value)
        }

        private fun indexOf(searchArray: ByteArray, toBeFound: Byte): Int {
            for (i in searchArray.indices) {
                if (searchArray[i] == toBeFound)
                    return i;
            }
            return -1;
        }

        private fun ByteArray.toHexString() = asUByteArray().joinToString(":") { it.toString(16).padStart(2, '0') }

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
        val hexString = deviceAddress.replace(":", "")
        return hexStringToByteArray(hexString) + ByteBuffer.allocate(4).putFloat(value).array()
    }

    private fun stringToByteAddress(stringAddress: String, numBytes: Int): ByteArray {
        var bytes = mutableListOf<Byte>()
        var start = 0
        for (i in stringAddress.indices) {
            if (stringAddress[i] == ':' || i == stringAddress.length - 1) {
                bytes.add(stringAddress.slice(start until i).toByte())
                start = i + 1
            }
        }
        return bytes.toByteArray();
    }
/*
    private fun byteAddressToString(bytes: ByteArray): String {
        var string = mutableListOf<Char>()
        for (byte in bytes) {
            string.add(by)
        }
        return bytes.toString(Charsets.UTF_8)
    }
*/
    private fun hexStringToByteArray(s: String): ByteArray {
        val len = s.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(s[i], 16) shl 4)
                    + Character.digit(s[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}