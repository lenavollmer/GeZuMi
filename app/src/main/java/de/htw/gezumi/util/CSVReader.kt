package de.htw.gezumi.util


import android.content.Context
import android.util.Log
import com.github.doyaaaaaken.kotlincsv.dsl.csvReader


private const val TAG = "CSVReader"

class CSVReader {
    companion object {
        fun getTxPower(deviceId: String, context: Context): Short {
            val inputStream = context.assets.open("en-calibration.csv")
            val device = csvReader().readAll(inputStream).find { row -> row[1] == deviceId }
            if (device == null) {
                Log.d(TAG, "------ NO TXPOWER AVAILABLE ------")
                return -23
            }
            Log.d(TAG, "txPower for ${device[1]} is ${device[4]}")
            return device[4].toShort()
        }
    }
}