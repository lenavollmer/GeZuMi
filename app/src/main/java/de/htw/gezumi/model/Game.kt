package de.htw.gezumi.model

import android.graphics.Point
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import de.htw.gezumi.calculation.Geometry
import de.htw.gezumi.calculation.Vec

private const val TAG = "Game"

class Game {

    val numberOfPlayers: Int get() = _players.value?.size ?: 0

    // contains a player for myself
    private val _players = MutableLiveData<MutableList<Player>>(
        mutableListOf()
    )

    val players: LiveData<MutableList<Player>> = _players

    private var _targetShape = Geometry.generateGeometricObject(numberOfPlayers)
    val targetShape: List<Vec> get() = _targetShape.map { Vec(it) }

    private val _targetShapeAnimation = MutableLiveData<List<Point>>()
    val targetShapeAnimation: MutableLiveData<List<Point>> get() = _targetShapeAnimation

    // Determines whether the target shape has been matched by the players
    private var _shapeMatched = MutableLiveData<Boolean>(false)
    val shapeMatched: MutableLiveData<Boolean> get() = _shapeMatched

    private var _time = 0
    val time: Int get() = _time

    // stopwatch running?
    private var _running = false
    val running: Boolean get() = _running

    private var _currentIdx = 0

    private val _animationPointsArray = generateTargetShapeAnimationPoints()
    val animationPointsArray: Array<List<Point>> get() = _animationPointsArray

//    fun setPlayerLocations(locations: List<Point>) {
//        _players.postValue(locations)
//    }

    fun setShapeMatched(matchedShape: Boolean) {
        Log.d("TAG", "I'm in setShapeMatched: $matchedShape")
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
        Log.d("TAG", "I'm in here")
        setRunning(false)
        setTime(0)
        resetCurrentIdx()
        _targetShape = Geometry.generateGeometricObject(numberOfPlayers)
        _shapeMatched.value = false
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

    /**
     * Add player if they do not exist.
     */
    fun addPlayerIfNew(deviceId: ByteArray) {
        if (!_players.value?.any {it.deviceId contentEquals deviceId}!!)
            (_players.value as MutableList<Player>).add(Player(deviceId))
    }

    fun getPlayer(deviceId: ByteArray): Player? = _players.value?.find{ it.deviceId contentEquals deviceId }

    /**
     * Add player if they do not exist. Update player position.
     */
    fun updatePlayer(deviceId: ByteArray, position: Vec) {
        addPlayerIfNew(deviceId)
        _players.value?.find {it.deviceId contentEquals deviceId}!!.position = position
        _players.postValue(_players.value) // TODO find better way needed to refresh the observer
    }

    fun clear() {
        // add more stuff here
        _players.value?.clear()
    }
}