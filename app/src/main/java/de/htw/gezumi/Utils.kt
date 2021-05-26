package de.htw.gezumi

class Utils {
    companion object {

        fun decodeHex(hexString: String): ByteArray {
            require(hexString.length % 2 == 0) { "Must have an even length" }
            return hexString.chunked(2)
                .map { it.toInt(16).toByte() }
                .toByteArray()
        }
    }
}