package de.htw.gezumi.gatt

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import java.nio.charset.Charset

import java.util.Calendar
import java.util.UUID

object GameService {

    val GAME_ID_LENGTH = 15;
    val CLIENT_UUID: UUID = UUID.fromString("00001805-0000-1000-8000-00805f9b34fb")
    val SERVER_UUID: UUID = UUID.fromString("00001805-0000-1000-8000-00805f9b34fc")
    val GAME_ID: UUID = UUID.fromString("00002a2b-0000-1000-8000-00805f9b34fb")
    /* Mandatory Client Characteristic Config Descriptor */
    val CLIENT_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")


    fun createGameService(uuid: UUID): BluetoothGattService {
        val service = BluetoothGattService(
            uuid,
            BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val gameId = BluetoothGattCharacteristic(GAME_ID, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ)

        /*
        val configDescriptor = BluetoothGattDescriptor(
            CLIENT_CONFIG,
            //Read/write descriptor
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE)
        currentTime.addDescriptor(configDescriptor)*/


        //service.addCharacteristic(currentTime)
        service.addCharacteristic(gameId)
        return service
    }

    fun getGameId(): ByteArray {
        return getRandomString(GAME_ID_LENGTH).toByteArray(Charsets.UTF_8)
    }

    private fun getRandomString(length: Int) : String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }
}