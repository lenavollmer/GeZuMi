package de.htw.gezumi.distance.filter

class MedianFilter : Filter {
    private val _values = mutableListOf<Double>()

    override fun applyFilter(rssi: Double): Double {
        _values.add(rssi)

        // only use the 10 most recent values
        if (_values.size > 10) _values.removeFirst()

        val sortedArray = _values.sorted()

        return if (sortedArray.size % 2 == 0) (sortedArray[sortedArray.size / 2].toDouble() + sortedArray[
                sortedArray.size / 2 - 1].toDouble()) / 2
        else sortedArray[sortedArray.size / 2].toDouble()
    }
}