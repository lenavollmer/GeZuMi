package de.htw.gezumi.model

import android.graphics.Point
import android.os.Handler
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import de.htw.gezumi.calculation.Geometry

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

    private var _targetShape = Geometry.generateGeometricObject(_players)
//    listOf(
//        Point(0, 0),
//        Point(200, 0),
//        Point(100, 200)
//    )
    val targetShape: List<Point> get() = _targetShape

    private val _targetShapeAnimation = MutableLiveData<List<Point>>()
    val targetShapeAnimation: MutableLiveData<List<Point>> get() = _targetShapeAnimation

    // Determines whether the target shape has been matched by the players
    private var _shapeMatched = MutableLiveData<Boolean>(false)
    val shapeMatched: LiveData<Boolean> get() = _shapeMatched

    private var _time = 0
    val time: Int get() = _time

    // stopwatch running?
    private var _running = false
    val running: Boolean get() = _running

    private var _currentIdx = 0

    private val _animationPointsArray = generateTargetShapeAnimationPoints()
    val animationPointsArray: Array<List<Point>> get() = _animationPointsArray

    fun setPlayerLocations(locations: List<Point>) {
        _playerLocations.postValue(locations)
    }

    fun setShapeMatched(matchedShape: Boolean) {
        _shapeMatched.postValue(matchedShape)
    }

    private fun setTargetShapeAnimation(points: List<Point>) {
        _targetShapeAnimation.postValue(points)
    }

    fun setTime(time: Int) {
        _time = time
    }

    fun setRunning(running: Boolean) {
        _running = running
    }

    fun resetCurrentIdx() {
        _currentIdx = 0
    }

    fun changeTargetLocationsLogic() {
        if (_currentIdx < 12 && _shapeMatched.value!!) {
            setTargetShapeAnimation(_animationPointsArray[_currentIdx])
            _currentIdx++
        }
    }

    fun resetState() {
        setRunning(false)
        setShapeMatched(false)
        setTime(0)
        resetCurrentIdx()
        _targetShape = Geometry.generateGeometricObject(_players)
    }

    private fun generateTargetShapeAnimationPoints(): Array<List<Point>> {
        val array: Array<List<Point>> = Array<List<Point>>(12) { _targetShape }

        for (int: Int in 0..4) {
            val current = array[int]
            array[int + 1] = current.map { it -> Point(it.x / 2, it.y / 2) }
        }

        for (int: Int in 4..(array.size - 2)) {
            val current = array[int]
            array[int + 1] = current.map { it -> Point(it.x * 2, it.y * 2) }
        }

        return array
    }
}