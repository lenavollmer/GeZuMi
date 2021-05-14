package de.htw.gezumi.gatt

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.util.Log
import java.util.*


private const val TAG = "GameService"

object GameService {

    val GAME_ID_LENGTH = 15;
    val HOST_UUID: UUID = UUID.fromString("00001805-0000-1000-8000-00805f9b34fc")
    val GAME_ID_UUID: UUID = UUID.fromString("00002a2b-0000-1000-8000-00805f9b34fb")
    val RSSI_UUID: UUID = UUID.fromString("00002a2b-0000-1000-8000-00805f9b34fa")
    val JOIN_APPROVED_UUID: UUID = UUID.fromString("00002a2b-0000-1000-8000-00805f9b34aa")
    val RSSI_SEND_REQUEST_UUID: UUID = UUID.fromString("00002a2b-0000-1000-8000-00805f9b34aa")
    /* Mandatory Client Characteristic Config Descriptor */
    val CLIENT_CONFIG: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

    val gameId = getRandomUuidString()

    fun createGameService(uuid: UUID): BluetoothGattService {
        val service = BluetoothGattService(
            uuid,
            BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val gameIdCharacteristic = BluetoothGattCharacteristic(GAME_ID_UUID, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ)

        val rssi = BluetoothGattCharacteristic(RSSI_UUID, BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, BluetoothGattCharacteristic.PERMISSION_WRITE)
        val joinApproved = BluetoothGattCharacteristic(JOIN_APPROVED_UUID, BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_WRITE)
        //val rssiDevice = BluetoothGattDescriptor(RSSI_SEND_REQUEST_UUID, BluetoothGattCharacteristic.PERMISSION_WRITE)
        //rssi.addDescriptor(rssiDevice)
        /*
        val configDescriptor = BluetoothGattDescriptor(
            CLIENT_CONFIG,
            //Read/write descriptor
            BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE)
        currentTime.addDescriptor(configDescriptor)*/

        service.addCharacteristic(gameIdCharacteristic)
        service.addCharacteristic(rssi)
        service.addCharacteristic(joinApproved)
        return service
    }

    fun getGameId(): ByteArray {
        //return getRandomString(GAME_ID_LENGTH).toByteArray(Charsets.UTF_8)
        ///return UUID.randomUUID().toString().toByteArray(Charsets.UTF_8) is invalid
        //val s = getRandomUuidString()
        //Log.d("GameService", "generated game id: $s")
        val bytes = gameId.toByteArray(Charsets.UTF_8)
        Log.d(TAG, "game id bytes: ${bytes.size}")
        return bytes
        //return "abc".toByteArray(Charsets.UTF_8)
    }

    private fun getRandomUuidString() : String {
        return getRandomString( 8) + "-" +
                getRandomString( 4) + "-" +
                getRandomString( 4) + "-" +
                getRandomString( 4) + "-" +
                getRandomString( 12)
    }

    private fun getRandomString(length: Int): String {
        val allowedChars = ('a'..'f') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }
}