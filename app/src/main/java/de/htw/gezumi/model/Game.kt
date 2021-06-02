package de.htw.gezumi.model

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import de.htw.gezumi.calculation.Vec

private const val TAG = "Game"

class Game {

    val numberOfPlayers: Int get() = _players.value?.size ?: 0

    // contains a player for myself
    private val _players = MutableLiveData<MutableList<Player>>(
        mutableListOf()
    )

    val players: LiveData<MutableList<Player>> = _players

    private val _targetShape = listOf(
        Vec(0, 0),
        Vec(2, 0),
        Vec(1, 2)
    )
    val targetShape: List<Vec> get() = _targetShape

    // Determines whether the target shape has been matched by the players
    private var _shapeMatched: Boolean = false
    val shapeMatched: Boolean get() = _shapeMatched

    private var _time = 0
    val time: Int get() = _time

//    fun setPlayerLocations(locations: List<Vec>) {
//        _players.postValue(locations)
//    }

    fun setShapeMatched(matchedShape: Boolean) {
        _shapeMatched = matchedShape
    }

    fun setTime(time: Int) {
        _time = time
    }

    /**
     * Add player if not exists.
     */
    fun addPlayerIfNew(deviceId: ByteArray) {
        if (!_players.value?.any {it.deviceId contentEquals deviceId}!!)
            (_players.value as MutableList<Player>).add(Player(deviceId))
    }

    fun getPlayer(deviceId: ByteArray): Player? = _players.value?.find{ it.deviceId contentEquals deviceId }

    /**
     * Add player if not exists. Update player position.
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