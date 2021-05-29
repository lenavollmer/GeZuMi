package de.htw.gezumi.filter

class MedianFilter: Filter {
    private val _values = mutableListOf<Float>()

    override fun applyFilter(rssi: Float): Float {
        _values.add(rssi)

        // only use the 10 most recent values
        if (_values.size > 10) _values.removeFirst()

        val sortedArray = _values.sorted()

        return if (sortedArray.size % 2 == 0) (sortedArray[sortedArray.size / 2] + sortedArray[
                sortedArray.size / 2 - 1]) / 2f
        else sortedArray[sortedArray.size / 2]
    }
}