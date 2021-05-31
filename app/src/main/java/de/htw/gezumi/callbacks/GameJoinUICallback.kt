package de.htw.gezumi.callbacks

interface GameJoinUICallback {
    fun gameJoined()
    fun gameDeclined()
    fun gameStarted()
    fun gameLeft()
}