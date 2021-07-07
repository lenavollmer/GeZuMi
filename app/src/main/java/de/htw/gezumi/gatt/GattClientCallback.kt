package de.htw.gezumi.gatt

import android.annotation.SuppressLint
import android.bluetooth.*
import android.util.Log
import de.htw.gezumi.Utils
import de.htw.gezumi.calculation.Vec
import de.htw.gezumi.model.BluetoothData
import de.htw.gezumi.util.Constants
import de.htw.gezumi.viewmodel.GameViewModel
import de.htw.gezumi.viewmodel.RANDOM_GAME_ID_PART_LENGTH
import java.nio.ByteBuffer
import java.util.*

private const val TAG = "ClientGattCallback"

class GattClientCallback() : BluetoothGattCallback() {

    @kotlin.ExperimentalUnsignedTypes
    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            Log.d(TAG, "callback: connected")
            Log.d(TAG, "discover services")
            gatt?.discoverServices()
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        super.onServicesDiscovered(gatt, status)
        Log.d(TAG, "services discovered")
        gatt!!.setCharacteristicNotification(
            gatt.getService(GameService.HOST_UUID)
                ?.getCharacteristic(GameService.JOIN_APPROVED_UUID), true
        )
        val joinName = gatt.getService(GameService.HOST_UUID).getCharacteristic(GameService.JOIN_NAME_UUID)
        joinName.value = GameViewModel.instance.playerName?.toByteArray(Charsets.UTF_8) ?: ByteArray(0)
        gatt.writeCharacteristic(joinName)
    }

    @kotlin.ExperimentalUnsignedTypes
    @SuppressLint("DefaultLocale")
    override fun onCharacteristicRead(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        super.onCharacteristicRead(gatt, characteristic, status)
        when (characteristic?.uuid) {
            GameService.GAME_ID_UUID -> {
                val randomIdPart =
                    characteristic.value.sliceArray(0 until RANDOM_GAME_ID_PART_LENGTH)
                GameViewModel.instance.gameId = GameService.GAME_ID_PREFIX + randomIdPart
                // TODO: also set GameService vars? for sure: decode gameName and display
                Log.d(
                    TAG,
                    "callback: characteristic read successfully, gameId: ${GameViewModel.instance.gameId}"
                )
                GameViewModel.instance.onGameJoin()
                Log.d(TAG, "subscribe for game events and host updates")
                val subscribeDescriptor = gatt?.getService(GameService.HOST_UUID)
                    ?.getCharacteristic(GameService.GAME_EVENT_UUID)
                    ?.getDescriptor(GameService.CLIENT_CONFIG)
                subscribeDescriptor?.value = BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                gatt?.writeDescriptor(subscribeDescriptor)
            }
        }
    }

    override fun onDescriptorWrite(
        gatt: BluetoothGatt?,
        descriptor: BluetoothGattDescriptor?,
        status: Int
    ) {
        super.onDescriptorWrite(gatt, descriptor, status)
        when (descriptor?.uuid) {
            // subscribe to game events and host updates
            GameService.CLIENT_CONFIG -> {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (Arrays.equals(
                            descriptor.value,
                            BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
                        )
                    ) {
                        Log.d(TAG, "game event and host update subscribe successful")
                        gatt?.setCharacteristicNotification(
                            gatt.getService(GameService.HOST_UUID)
                                ?.getCharacteristic(GameService.GAME_EVENT_UUID), true
                        )
                        gatt?.setCharacteristicNotification(
                            gatt.getService(GameService.HOST_UUID)
                                ?.getCharacteristic(GameService.HOST_UPDATE_UUID), true
                        )
                        gatt?.setCharacteristicNotification(
                            gatt.getService(GameService.HOST_UUID)
                                ?.getCharacteristic(GameService.RESPONSE_HOST_UPDATE_UUID), true
                        )
                        Log.d(TAG, "send identification to host")
                        val identificationCharacteristic = gatt?.getService(GameService.HOST_UUID)
                            ?.getCharacteristic(GameService.PLAYER_IDENTIFICATION_UUID)
                        identificationCharacteristic!!.value = GameViewModel.instance.myDeviceId
                        gatt.writeCharacteristic(identificationCharacteristic)
                    } else if (Arrays.equals(
                            descriptor.value,
                            BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                        )
                    ) {
                        Log.d(TAG, "game event unsubscribe successful")
                        gatt?.setCharacteristicNotification(
                            gatt.getService(GameService.HOST_UUID)
                                ?.getCharacteristic(GameService.GAME_EVENT_UUID), false
                        )
                        gatt?.setCharacteristicNotification(
                            gatt.getService(GameService.HOST_UUID)
                                ?.getCharacteristic(GameService.HOST_UPDATE_UUID), false
                        )
                        gatt?.setCharacteristicNotification(
                            gatt.getService(GameService.HOST_UUID)
                                ?.getCharacteristic(GameService.RESPONSE_HOST_UPDATE_UUID), false
                        )
                    }
                }
            }
        }
    }

    @kotlin.ExperimentalUnsignedTypes
    @SuppressLint("DefaultLocale")
    override fun onCharacteristicChanged(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?
    ) {
        super.onCharacteristicChanged(gatt, characteristic)
        when (characteristic?.uuid) {
            GameService.JOIN_APPROVED_UUID -> {
                val approved = ByteBuffer.wrap(characteristic.value).int
                if (approved == 1) {
                    Log.d(TAG, "approved, read game id")
                    val gameIdCharacteristic = gatt?.getService(GameService.HOST_UUID)
                        ?.getCharacteristic(GameService.GAME_ID_UUID)
                    gatt?.readCharacteristic(gameIdCharacteristic)
                } else {
                    GameViewModel.instance.onGameDecline()
                }
            }
            GameService.GAME_EVENT_UUID -> {
                val event = ByteBuffer.wrap(characteristic.value).int
                if (event == GameService.GAME_START_EVENT) {
                    GameViewModel.instance.onGameStart()
                }
                if (event == GameService.GAME_END_EVENT) {
                    GameViewModel.instance.onGameLeave()
                }
            }
            GameService.RESPONSE_HOST_UPDATE_UUID,
            GameService.HOST_UPDATE_UUID -> {
                val bluetoothData = BluetoothData.fromBytes(characteristic.value)
                Log.d(
                    TAG,
                    "received host update from: ${Utils.toHexString(bluetoothData.senderId)} to device: ${
                        Utils.toHexString(bluetoothData.id)
                    } values=${bluetoothData.values.contentToString()}, size=${characteristic.value.size}"
                )
                if (bluetoothData.id contentEquals Constants.TARGET_SHAPE_ID) {
                    GameViewModel.instance.game.updateTargetShape(
                        Vec(bluetoothData.values[0], bluetoothData.values[1])
                    )
                } else if (bluetoothData.id contentEquals Constants.RESET_GAME_ID) {
                    Log.d(TAG, "I am in the reset game client callback situation")
                    GameViewModel.instance.game.restart()
                } else {
                    GameViewModel.instance.game.updatePlayer(
                        bluetoothData.id,
                        Vec(bluetoothData.values[0], bluetoothData.values[1])
                    )
                }
            }
        }
    }
}