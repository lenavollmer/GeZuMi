package de.htw.gezumi.util


import android.content.Context
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader


private const val TAG = "CSVReader"

class CSVReader {
    companion object {
        fun getTxPower(deviceId: String, context: Context): Float {
            val inputStream = context.assets.open("en-calibration.csv")
            val device = csvReader().readAll(inputStream).find { row -> row[1] == deviceId }
            if (device != null) return device[4].toFloat()
            return -20f
        }
    }
}