package de.htw.gezumi.model

import android.graphics.Point
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class Game() {
    // Set numbers of players = currently fixed to three
    private val _players = 3
    val players: Int get() = _players

    private val _playerLocations = MutableLiveData<List<Point>>(
        listOf(
            Point(0, 0),
            Point(200, 0),
            Point(100, 200)
        )
    )
    val playerLocations: LiveData<List<Point>> get() = _playerLocations

    private val _targetShape = listOf(
        Point(0, 0),
        Point(200, 0),
        Point(100, 200)
    )
    val targetShape: List<Point> get() = _targetShape

    // Determines whether the target shape has been matched by the players
    private var _shapeMatched: Boolean = false
    val shapeMatched: Boolean get() = _shapeMatched

    private var _time = 0
    val time: Int get() = _time

    fun setPlayerLocations(locations: List<Point>) {
        _playerLocations.postValue(locations)
    }

    fun setShapeMatched(matchedShape: Boolean) {
        _shapeMatched = matchedShape
    }

    fun setTime(time: Int) {
        _time = time
    }
}