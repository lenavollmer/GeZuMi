package de.htw.gezumi.model

import android.graphics.Point
import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import de.htw.gezumi.calculation.Geometry
import de.htw.gezumi.calculation.Vec

private const val TAG = "Game"

class Game(private val hostId: ByteArray?) {

    val numberOfPlayers: Int get() = _players.value?.size ?: 0

    // contains a player for myself
    private val _players = MutableLiveData<MutableList<Player>>(
        mutableListOf()
    )
    val players: LiveData<MutableList<Player>> = _players

    private var _targetShape = listOf(Vec(0f, 0f), Vec(2f, 0f), Vec(1f, 2f))
    val targetShape: List<Vec> get() = _targetShape

    private val _targetShapeAnimation = MutableLiveData<List<Vec>>()
    val targetShapeAnimation: MutableLiveData<List<Vec>> get() = _targetShapeAnimation

    // Determines whether the target shape has been matched by the players
    private var _shapeMatched = MutableLiveData(false)
    val shapeMatched: MutableLiveData<Boolean> get() = _shapeMatched

    private var _time = 0
    val time: Int get() = _time

    // stopwatch running?
    private var _running = false
    val running: Boolean get() = _running

    private var _currentIdx = 0

    private val _animationPointsArray = generateTargetShapeAnimationPoints()
    val animationPointsArray: Array<List<Vec>> get() = _animationPointsArray

//    fun setPlayerLocations(locations: List<Point>) {
//        _players.postValue(locations)
//    }

    fun setShapeMatched(matchedShape: Boolean) {
        Log.d("TAG", "I'm in setShapeMatched: $matchedShape")
        _shapeMatched.postValue(matchedShape)
    }

    private fun setTargetShapeAnimation(points: List<Vec>) {
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
            Log.d(TAG, "targetShape changeTargetLocationsLogic")
            setTargetShapeAnimation(_animationPointsArray[_currentIdx])
            _currentIdx++
        }
    }

    fun resetState() {
        setRunning(false)
        setTime(0)
        resetCurrentIdx()
        _targetShape = listOf(Vec(0f, 0f), Vec(2f, 0f), Vec(1f, 2f))
        _shapeMatched.value = false
    }

    private fun generateTargetShapeAnimationPoints(): Array<List<Vec>> {
        val array: Array<List<Vec>> = Array(12) { _targetShape }

        for (int: Int in 0..4) {
            val current = array[int]
            array[int + 1] = current.map { Vec(it.x / 2, it.y / 2) }
        }

        for (int: Int in 4..(array.size - 2)) {
            val current = array[int]
            array[int + 1] = current.map { Vec(it.x * 2, it.y * 2) }
        }

        return array
    }

    /**
     * Add player if they do not exist.
     */
    fun addPlayerIfNew(deviceId: ByteArray) {
        if (players.value !== null && !_players.value!!.any { it.deviceId contentEquals deviceId }) {
            // make sure that the host is always at index 0 of players
            if (deviceId contentEquals hostId) {
                (_players.value as MutableList<Player>).add(0, Player(deviceId))
            }
            (_players.value as MutableList<Player>).add(Player(deviceId))
        }
    }

    fun getPlayer(deviceId: ByteArray): Player? = _players.value?.find { it.deviceId contentEquals deviceId }

    /**
     * Add player if they do not exist. Update player position.
     */
    fun updatePlayer(deviceId: ByteArray, position: Vec) {
        addPlayerIfNew(deviceId)
        _players.value?.find { it.deviceId contentEquals deviceId }!!.position = position
        _players.postValue(_players.value)
    }

    fun clear() {
        // add more stuff here
        _players.value?.clear()
    }
}