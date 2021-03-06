package de.htw.gezumi.model

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import de.htw.gezumi.calculation.Vec

private const val TAG = "Game"

class Game {
    var hostId: ByteArray? = null

    private val _players = MutableLiveData<MutableList<Player>>(
        mutableListOf()
    )
    val players: LiveData<MutableList<Player>> = _players

    private var _targetShape = MutableLiveData<MutableList<Vec>>(mutableListOf())
    val targetShape: MutableLiveData<MutableList<Vec>> get() = _targetShape

    // Determines whether the target shape has been matched by the players
    private var _shapeMatched = MutableLiveData(false)
    val shapeMatched: MutableLiveData<Boolean> get() = _shapeMatched

    var time = 0
    var running = false

    fun setShapeMatched(matchedShape: Boolean) {
        _shapeMatched.postValue(matchedShape)
    }

    fun setTargetShape(points: MutableList<Vec>) {
        _targetShape.postValue(points)
    }

    fun restart() {
        time = 0
        _targetShape.value?.clear()
        _shapeMatched.postValue(false)
        running = false
    }

    /**
     * Kills game and resets all states.
     */
    fun reset() {
        running = false
        time = 0
        _targetShape.value?.clear()
        _players.value?.clear()
        _shapeMatched.postValue(false)
    }

    /**
     * Add player if they do not exist.
     */
    fun addPlayerIfNew(deviceId: ByteArray) {
        if (players.value !== null && !_players.value!!.any { it.deviceId contentEquals deviceId }) {
            // make sure that the host is always at index 0 of players
            Log.d(TAG, "hostId: $hostId deviceId:$deviceId")

            if (deviceId contentEquals hostId) {
                (_players.value as MutableList<Player>).add(0, Player(deviceId))
                Log.d(TAG, "Added host at beginning, hostid: $hostId first player id:${_players.value!![0].deviceId}")
            }
            (_players.value as MutableList<Player>).add(Player(deviceId))
        }
    }

    fun getPlayer(deviceId: ByteArray): Player? =
        _players.value?.find { it.deviceId contentEquals deviceId }

    /**
     * Add player if they do not exist. Update player position.
     */
    fun updatePlayer(deviceId: ByteArray, position: Vec) {
        addPlayerIfNew(deviceId)
        _players.value?.find { it.deviceId contentEquals deviceId }!!.position = position
        _players.postValue(_players.value)
    }

    /**
     * Add vectors to the target shape - called by clients.
     */
    fun updateTargetShape(vec: Vec) {
        Log.d(TAG, "new target position: $vec")
        if (!_targetShape.value!!.contains(vec)) {
            val currentTarget = _targetShape.value!!
            currentTarget.add(vec)
            setTargetShape(currentTarget)
        }
    }
}