package de.htw.gezumi.gatt

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.util.Log
import de.htw.gezumi.Utils
import java.util.*


private const val TAG = "GameService"

object GameService {

    const val GAME_START_EVENT = 0

    val HOST_UUID: UUID = UUID.fromString("4a92d33c-0000-1000-8000-00805f9b34fc")
    val GAME_ID_UUID: UUID = UUID.fromString("4a92d33c-0000-1000-8000-00805f9b34fb")
    val PLAYER_UPDATE_UUID: UUID = UUID.fromString("4a92d33c-0000-1000-8000-00805f9b51ab")
    val HOST_UPDATE_UUID: UUID = UUID.fromString("4a92d33c-0000-1000-8000-00805f9b34fa") // all game_event subscribed device also get host updates
    val JOIN_APPROVED_UUID: UUID = UUID.fromString("4a92d33c-0000-1000-8000-00805f9b34aa")
    val GAME_EVENT_UUID: UUID = UUID.fromString("4a92d33c-0000-1000-8000-00805f9b34ab")
    val CLIENT_CONFIG: UUID = UUID.fromString("4a92d33c-0000-1000-8000-00805f9b34fb")

    val GAME_ID_PREFIX = Utils.decodeHex("4a92d33c")
    // the random part of the game id
    val randomIdPart = ByteArray(2)
    lateinit var gameName: String

    fun createHostService(): BluetoothGattService {
        // is host: initialize random id part
        Random().nextBytes(randomIdPart)


        val service = BluetoothGattService(
            HOST_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val gameIdCharacteristic = BluetoothGattCharacteristic(GAME_ID_UUID, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ)

        val playerUpdate = BluetoothGattCharacteristic(PLAYER_UPDATE_UUID, BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, BluetoothGattCharacteristic.PERMISSION_WRITE)
        val hostUpdate = BluetoothGattCharacteristic(HOST_UPDATE_UUID, BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, BluetoothGattCharacteristic.PERMISSION_WRITE)
        val joinApproved = BluetoothGattCharacteristic(JOIN_APPROVED_UUID, BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ)
        val gameEvent = BluetoothGattCharacteristic(GAME_EVENT_UUID, BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ)

        val configDescriptor = BluetoothGattDescriptor(CLIENT_CONFIG, BluetoothGattDescriptor.PERMISSION_WRITE)
        gameEvent.addDescriptor(configDescriptor)

        service.addCharacteristic(gameIdCharacteristic)
        service.addCharacteristic(playerUpdate)
        service.addCharacteristic(hostUpdate)
        service.addCharacteristic(joinApproved)
        service.addCharacteristic(gameEvent)
        return service
    }

    //fun getGameId(): String = GAME_ID_PREFIX + gameIdPostfix

    private fun getRandomUuidString(length: Int): String {
        val allowedChars = ('a'..'f') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }
}