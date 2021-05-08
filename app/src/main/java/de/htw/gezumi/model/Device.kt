package de.htw.gezumi.model

import de.htw.gezumi.distance.Calc
import de.htw.gezumi.distance.filter.MedianFilter
import de.htw.gezumi.distance.filter.Filter


class Device(
    val id: String = "Unknown ID",
    var name: String = "Unknown Name",
    private val _txPower: Double = -71.0
) {

    private val _filter: Filter = MedianFilter()
    var distance: Double? = null

    fun addRSSI(rssi: Int) {
        val curDist = Calc.rssiToDistance(rssi.toDouble(), _txPower)
        distance = _filter.applyFilter(curDist)
    }
}