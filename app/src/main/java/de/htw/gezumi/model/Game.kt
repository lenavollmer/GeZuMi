package de.htw.gezumi.model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import de.htw.gezumi.calculation.Vec

private const val TAG = "Game"

class Game(private val hostId: ByteArray?) {

    val numberOfPlayers: Int get() = _players.value?.size ?: 0

    // contains a player for myself
    private val _players = MutableLiveData<MutableList<Player>>(
        mutableListOf()
    )
    val players: LiveData<MutableList<Player>> = _players

    private var _targetShape = MutableLiveData<MutableList<Vec>>(
        mutableListOf(
            Vec(0, 0),
            Vec(2, 0),
            Vec(1, 2)
        )
    )
    val targetShape: MutableLiveData<MutableList<Vec>> get() = _targetShape

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

    private var _animationPointsArray: Array<List<Vec>> = arrayOf()
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

    fun setTargetShape(points: MutableList<Vec>) {
        _targetShape.postValue(points)
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
        setTime(0)
        resetCurrentIdx()
        // TODO generate new target shape and send to clients
        _shapeMatched.value = false
    }

    fun generateTargetShapeAnimationPoints() {
        val array: Array<List<Vec>> = Array(12) { _targetShape.value!! }

        for (int: Int in 0..4) {
            val current = array[int]
            array[int + 1] = current.map { Vec(it.x / 2, it.y / 2) }
        }

        for (int: Int in 4..(array.size - 2)) {
            val current = array[int]
            array[int + 1] = current.map { Vec(it.x * 2, it.y * 2) }
        }

        _animationPointsArray = array
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

    /**
     * Add vectors to the target shape - called by clients
     */
    fun updateTargetShape(vec: Vec) {
        !_targetShape.value!!.contains(vec)
        // if line 139 doesn't work use this instead:
        // _targetShape.value!!.find { it.x == vec.x && it.y == vec.y } == null
        if(!_targetShape.value!!.contains(vec)) {
            val currentTarget = _targetShape.value!!
            currentTarget.add(vec)
            setTargetShape(currentTarget)
        }
    }

    fun clear() {
        // add more stuff here
        _players.value?.clear()
    }
}