package de.htw.gezumi.filter


interface Filter {
    fun applyFilter(rssi: Float): Float
}
