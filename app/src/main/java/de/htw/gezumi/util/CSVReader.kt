package de.htw.gezumi.util


import android.content.Context
import android.util.Log
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader

class CSVReader {
    companion object {
        fun getTxPower(deviceId: String, context: Context): Short {
            val inputStream = context.assets.open("en-calibration.csv")
            val device = csvReader().readAll(inputStream).find { row -> row[1] == deviceId }
            if (device == null) {
                return -23
            }
            return device[4].toShort()
        }
    }
}