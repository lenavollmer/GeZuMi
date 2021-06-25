package de.htw.gezumi.calculation

import android.app.Application
import android.util.Log
import de.htw.gezumi.Utils
import de.htw.gezumi.filter.Filter
import de.htw.gezumi.filter.KalmanFilter
import de.htw.gezumi.filter.MedianFilter
import de.htw.gezumi.model.Device
import de.htw.gezumi.model.TAG
import de.htw.gezumi.viewmodel.GameViewModel
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.lang.NumberFormatException
import java.text.SimpleDateFormat
import java.util.*

object DistanceEmulator {

    val file = "device0_-25_log_outdoor_1.txt"
    val path = "C:/Users/samue/gezumi/"
    val txPower: Short = -25

    //val testDevice: Device = Device(byteArrayOf(), txPower, null)
    val timestamps = mutableListOf<String>()


    private val _filterKalman: Filter = KalmanFilter()
    private val _filterMedian: Filter = MedianFilter()

    val rssiHistoryUnfiltered = mutableListOf<Int>()
    val distanceHistoryUnfiltered = mutableListOf<Float>()

    val rssiHistoryKalman = mutableListOf<Float>()
    val distanceHistoryKalman = mutableListOf<Float>()

    val rssiHistoryMedian = mutableListOf<Float>()
    val distanceHistoryMedian = mutableListOf<Float>()

    @JvmStatic
    fun main(args: Array<String>) {
        val rssis = readRssis()
        for (i in rssis.indices) {
            //testDevice.addRssi(rssi)
            //Conversions.rssiToDistance(rssi.toFloat(), txPower)
            addRssi(rssis[i], timestamps[i])
        }
    }

    private fun addRssi(rssi: Int, timeString: String) {
        rssiHistoryUnfiltered.add(rssi)
        val distanceUnfiltered = Conversions.rssiToDistance(rssi.toFloat(), txPower)
        distanceHistoryUnfiltered.add(distanceUnfiltered)

        val filteredKalman = _filterKalman.applyFilter(rssi.toFloat())
        rssiHistoryKalman.add(filteredKalman)
        val distanceKalman = Conversions.rssiToDistance(filteredKalman, txPower)
        distanceHistoryKalman.add(distanceKalman)

        val filteredMedian = _filterMedian.applyFilter(rssi.toFloat())
        rssiHistoryMedian.add(filteredMedian)
        val distanceMedian = Conversions.rssiToDistance(filteredMedian, txPower)
        distanceHistoryMedian.add(distanceMedian)
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
                timestamps.add(cols[0])
        }
    }
}