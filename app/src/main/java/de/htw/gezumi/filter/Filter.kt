package de.htw.gezumi.filter


interface Filter {
    fun applyFilter(rssi: Double): Double
}
