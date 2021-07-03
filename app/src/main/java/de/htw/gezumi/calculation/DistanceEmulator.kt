package de.htw.gezumi.calculation

import de.htw.gezumi.model.Device
import java.io.BufferedReader
import java.io.File

object DistanceEmulator {

    val file = "device0_-25_log_outdoor_1.txt"
    val path = "C:/Users/samue/gezumi/"
    val txPower: Short = -14

    val testDevice: Device = Device(byteArrayOf(), txPower, null, true)
    private val _timestamps = mutableListOf<String>()

    @JvmStatic
    fun main(args: Array<String>) {
        val rssis = readRssis()
        for (i in rssis.indices) {
            testDevice.addRssi(rssis[i], _timestamps[i])
        }
        writeTestLog()
    }

    private fun writeTestLog() {
        val fname = "testDevice_log.txt"
        val myDir = File(path)
        if (!myDir.exists()) {
            myDir.mkdirs()
        }
        val file = File(myDir, fname)
        testDevice.writeRssiLog(file)
    }

    private fun readRssis(): List<Int> {
        val bufferedReader: BufferedReader = File(path + file).bufferedReader()
        //val inputString = bufferedReader.use { it.readText() }
        val rssis = mutableListOf<Int>()
        //csvReader().readAll(inputString).forEach { row -> rssis.add(Integer.parseInt(row[1])) }
        var line: String?
        while (bufferedReader.readLine().also { line = it } != null) {
            // use comma as separator
            val cols: List<String> = line!!.split(";")
            try {
                rssis.add(Integer.parseInt(cols[1]))
            } catch (e: NumberFormatException) {
                continue
            }
            if (cols[0] != "time_ms")
                _timestamps.add(cols[0])
        }
        return rssis
    }
}