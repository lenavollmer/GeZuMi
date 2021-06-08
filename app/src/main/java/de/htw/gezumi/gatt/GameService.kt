package de.htw.gezumi.gatt

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattService
import android.bluetooth.le.ScanResult
import android.util.Log
import de.htw.gezumi.Utils
import de.htw.gezumi.viewmodel.*
import java.util.*


private const val TAG = "GameService"

object GameService {

    const val GAME_START_EVENT = 0

    val HOST_UUID: UUID = UUID.fromString("4a92d33c-0000-1000-8000-00805f9b34fc")
    val GAME_ID_UUID: UUID = UUID.fromString("4a92d33c-0000-1000-8000-00805f9b34fb")
    val PLAYER_UPDATE_UUID: UUID = UUID.fromString("4a92d33c-0000-1000-8000-00805f9b51ab")
    val PLAYER_IDENTIFICATION_UUID: UUID = UUID.fromString("4a92d33c-0000-1000-8000-00805f9b128c")
    val PLAYER_NAME_UUID: UUID = UUID.fromString("4a92d33c-0000-1000-8000-00805f9b3156")
    val HOST_UPDATE_UUID: UUID = UUID.fromString("4a92d33c-0000-1000-8000-00805f9b34fa") // all game_event subscribed devices also get host updates
    val JOIN_APPROVED_UUID: UUID = UUID.fromString("4a92d33c-0000-1000-8000-00805f9b34aa")
    val GAME_EVENT_UUID: UUID = UUID.fromString("4a92d33c-0000-1000-8000-00805f9b34ab")
    val CLIENT_CONFIG: UUID = UUID.fromString("4a92d33c-0000-1000-8000-00805f9b34fb")

    val GAME_ID_PREFIX = Utils.decodeHex("4a92d31a") // any active players are not found in host scans
    val HOST_ID_PREFIX = Utils.decodeHex("4a92d3ad") // 4th byte is ignored in game scan, so host is found
    //val HOST_ID_PREFIX = Utils.decodeHex("a3df123c")
    // the random part of the game id
    val randomIdPart = ByteArray(RANDOM_GAME_ID_PART_LENGTH)
    lateinit var gameName: String

    fun createHostService(): BluetoothGattService {
        // is host: initialize random id part
        Random().nextBytes(randomIdPart)


        val service = BluetoothGattService(
            HOST_UUID,
            BluetoothGattService.SERVICE_TYPE_PRIMARY)
        val gameIdCharacteristic = BluetoothGattCharacteristic(GAME_ID_UUID, BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ)

        val playerUpdate = BluetoothGattCharacteristic(PLAYER_UPDATE_UUID, BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, BluetoothGattCharacteristic.PERMISSION_WRITE)
        val identification = BluetoothGattCharacteristic(PLAYER_IDENTIFICATION_UUID, BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, BluetoothGattCharacteristic.PERMISSION_WRITE)
        val playerName = BluetoothGattCharacteristic(PLAYER_UPDATE_UUID, BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, BluetoothGattCharacteristic.PERMISSION_WRITE)
        val hostUpdate = BluetoothGattCharacteristic(HOST_UPDATE_UUID, BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE, BluetoothGattCharacteristic.PERMISSION_WRITE)
        val joinApproved = BluetoothGattCharacteristic(JOIN_APPROVED_UUID, BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ)
        val gameEvent = BluetoothGattCharacteristic(GAME_EVENT_UUID, BluetoothGattCharacteristic.PROPERTY_NOTIFY, BluetoothGattCharacteristic.PERMISSION_READ)

        val configDescriptor = BluetoothGattDescriptor(CLIENT_CONFIG, BluetoothGattDescriptor.PERMISSION_WRITE)
        gameEvent.addDescriptor(configDescriptor)

        service.addCharacteristic(gameIdCharacteristic)
        service.addCharacteristic(playerUpdate)
        service.addCharacteristic(identification)
        service.addCharacteristic(playerName)
        service.addCharacteristic(hostUpdate)
        service.addCharacteristic(joinApproved)
        service.addCharacteristic(gameEvent)
        return service
    }

    fun extractName(result: ScanResult): String = result.scanRecord!!
        .getManufacturerSpecificData(76)!!
        .sliceArray(GAME_ID_LENGTH until GAME_ID_LENGTH + GAME_NAME_LENGTH)
        .filter{ byte -> byte != 0.toByte() }.toByteArray()
        .toString(Charsets.UTF_8)

    fun extractDeviceId(result: ScanResult): ByteArray = result.scanRecord!!
        .getManufacturerSpecificData(76)!!
        .sliceArray(DEVICE_ID_OFFSET until DEVICE_ID_OFFSET + DEVICE_ID_LENGTH)
}