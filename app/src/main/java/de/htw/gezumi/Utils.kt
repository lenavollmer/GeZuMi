package de.htw.gezumi

import android.annotation.SuppressLint

class Utils {
    companion object {

        fun decodeHex(hexString: String): ByteArray {
            require(hexString.length % 2 == 0) { "Must have an even length" }
            return hexString.chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
        }

        // TODO: does : separation
        @kotlin.ExperimentalUnsignedTypes
        @SuppressLint("DefaultLocale")
        fun toHexString(bytes: ByteArray) = bytes.asUByteArray()
            .joinToString(":") {
                it.toString(16).padStart(2, '0')
            }.toUpperCase()
    }
}