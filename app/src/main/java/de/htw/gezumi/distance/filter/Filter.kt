package de.htw.gezumi.distance.filter

interface Filter {
    fun applyFilter(rssi: Double): Double
}
