package de.htw.gezumi.callbacks

interface PlayerCallback {
    fun gameJoined()
    fun gameDeclined()
    fun gameStarted()
}