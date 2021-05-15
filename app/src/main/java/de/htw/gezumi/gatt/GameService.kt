package de.htw.gezumi.gatt

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.util.Log
import java.util.*


private const val TAG = "GameService"

object GameService {

    const val GAME_START_EVENT = 0

    val HOST_UUID: UUID = UUID.fromString("00002672-0000-1000-8000-00805f9b34fc")
    val GAME_ID_UUID: UUID = UUID.fromString("00002672-0000-1000-8000-00805f9b34fb")
    val RSSI_UUID: UUID = UUID.fromString("00002672-0000-1000-8000-00805f9b34fa")
    val JOIN_APPROVED_UUID: UUID = UUID.fromString("00002672-0000-1000-8000-00805f9b34aa")
    val GAME_EVENT_UUID: UUID = UUID.fromString("00002672-0000-1000-8000-00805f9b34ab")
    val CLIENT_CONFIG: UUID = UUID.fromString("00002672-0000-1000-8000-00805f9b34fb")

    const val GAME_ID_PREFIX = "00002672-0000-1000-"
    val gameId = getRandomUuidString()

    fun createGameService(uuid: UUID): BluetoothGattService {
        val service = BluetoothGattService(
            uuid,
            BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val gameIdCharacteristic = BluetoothGattCharacteristic(GAME_ID_UUID, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ)

        val rssi = BluetoothGattCharacteristic(RSSI_UUID, BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, BluetoothGattCharacteristic.PERMISSION_WRITE)
        val joinApproved = BluetoothGattCharacteristic(JOIN_APPROVED_UUID, BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ)
        val gameEvent = BluetoothGattCharacteristic(GAME_EVENT_UUID, BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ)

        val configDescriptor = BluetoothGattDescriptor(CLIENT_CONFIG,BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE)
        gameEvent.addDescriptor(configDescriptor)

        service.addCharacteristic(gameIdCharacteristic)
        service.addCharacteristic(rssi)
        service.addCharacteristic(joinApproved)
        service.addCharacteristic(gameEvent)
        return service
    }

    private fun getRandomUuidString() : String {
        Log.d("GameService", "generate 16 byte game id")
        return getRandomString( 4) + "-" +
                getRandomString( 12)
    }

    private fun getRandomString(length: Int): String {
        val allowedChars = ('a'..'f') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }
}