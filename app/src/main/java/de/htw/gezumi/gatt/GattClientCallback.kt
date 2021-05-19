package de.htw.gezumi.gatt

import android.bluetooth.*
import android.util.Log
import de.htw.gezumi.viewmodel.GameViewModel
import java.nio.ByteBuffer
import java.util.*

private const val TAG = "ClientGattCallback"

class GattClientCallback(private val _gameViewModel: GameViewModel) : BluetoothGattCallback() {

    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            Log.d(TAG, "callback: connected")
            Log.d(TAG, "discover services")
            gatt?.discoverServices()

        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
        }
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        super.onServicesDiscovered(gatt, status)
        Log.d(TAG, "services discovered")
        gatt?.setCharacteristicNotification(gatt.getService(GameService.HOST_UUID)?.getCharacteristic(GameService.JOIN_APPROVED_UUID), true)
    }

    override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
        super.onCharacteristicRead(gatt, characteristic, status)
        when (characteristic?.uuid) {
            GameService.GAME_ID_UUID -> {
                val gameIdPostfix = characteristic.value.toString(Charsets.UTF_8)
                _gameViewModel.gameId = UUID.fromString(GameService.GAME_ID_PREFIX + gameIdPostfix)
                Log.d(TAG, "callback: characteristic read successfully, gameId: ${_gameViewModel.gameId}")
                _gameViewModel.onGameJoin()
                Log.d(TAG, "subscribe for game events")
                val subscribeDescriptor = gatt?.getService(GameService.HOST_UUID)?.getCharacteristic(GameService.GAME_EVENT_UUID)?.getDescriptor(GameService.CLIENT_CONFIG)
                subscribeDescriptor?.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                gatt?.writeDescriptor(subscribeDescriptor)
            }
        }
    }

    override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
        super.onDescriptorWrite(gatt, descriptor, status)
        when (descriptor?.uuid) {
            GameService.CLIENT_CONFIG -> {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (Arrays.equals(descriptor.value, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                        Log.d(TAG, "game event subscribe successful")
                        gatt?.setCharacteristicNotification(gatt.getService(GameService.HOST_UUID)?.getCharacteristic(GameService.GAME_EVENT_UUID), true)
                    }
                    else if (Arrays.equals(descriptor.value, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
                        Log.d(TAG, "game event unsubscribe successful")
                        gatt?.setCharacteristicNotification(gatt.getService(GameService.HOST_UUID)?.getCharacteristic(GameService.GAME_EVENT_UUID), false)
                    }
                }
            }
        }
    }

    override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
        super.onCharacteristicChanged(gatt, characteristic)
        when (characteristic?.uuid) {
            GameService.JOIN_APPROVED_UUID -> {
                Log.d(TAG, "callback")
                val approved = ByteBuffer.wrap(characteristic.value).int
                if (approved == 1) {
                    Log.d(TAG, "approved, read game id")
                    val gameIdCharacteristic = gatt?.getService(GameService.HOST_UUID)?.getCharacteristic(GameService.GAME_ID_UUID)
                    gatt?.readCharacteristic(gameIdCharacteristic)
                }
                else {
                    _gameViewModel.onGameDecline()
                }
            }
            GameService.GAME_EVENT_UUID -> {
                val event = ByteBuffer.wrap(characteristic.value).int
                if(event == GameService.GAME_START_EVENT){
                    _gameViewModel.onGameStart()
                }
            }
        }
    }

}