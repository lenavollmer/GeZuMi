package de.htw.gezumi

import android.annotation.SuppressLint
import de.htw.gezumi.model.Device
import java.util.*

class Utils {
    companion object {

        fun decodeHex(hexString: String): ByteArray {
            require(hexString.length % 2 == 0) { "Must have an even length" }
            return hexString.chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
        }

        @kotlin.ExperimentalUnsignedTypes
        @SuppressLint("DefaultLocale")
        fun toHexString(bytes: ByteArray) = bytes.asUByteArray()
            .joinToString(":") {
                it.toString(16).padStart(2, '0')
            }.toUpperCase()

        fun contains(list: List<Device>, device: Device): Boolean {
            return contains(list, device.deviceId)
        }

        fun contains(list: List<Device>, id: ByteArray): Boolean {
            return list.any { d -> d.deviceId.contentEquals(id) }
        }

        fun findDevice(list: List<Device>, id: ByteArray): Device? {
            return list.find { d -> d.deviceId.contentEquals(id) }
        }

        fun findDeviceIndex(list: List<Device>, id: ByteArray): Int {
            return list.indexOfFirst { d -> d.deviceId.contentEquals(id) }
        }

        @kotlin.ExperimentalUnsignedTypes
        fun logDeviceId(arr: ByteArray) = toHexString(arr)
    }
}