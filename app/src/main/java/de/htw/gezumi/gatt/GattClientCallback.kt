package de.htw.gezumi.gatt

import android.bluetooth.*
import android.util.Log
import de.htw.gezumi.GameFragment
import de.htw.gezumi.model.DeviceData
import de.htw.gezumi.viewmodel.GameViewModel
import java.nio.ByteBuffer
import java.util.*

private const val TAG = "ClientGattCallback"

class GattClientCallback(private val _gameViewModel: GameViewModel, private val gameJoinCallback: GameFragment.GameJoinCallback) : BluetoothGattCallback() {

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

/*
bitte noch nicht löschen :)
    override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
        super.onReadRemoteRssi(gatt, rssi, status)
        _gameViewModel.devices[0].addRssi(rssi) // TODO set for correct device
        // TODO the raw rssi value is transferred here, but a processed value should
        _lastRssi = rssi
        // write device the rssi was measured for (in this test case it's just the host)
        //val sendRequest = gatt?.getService(GameService.SERVER_UUID)?.getCharacteristic(GameService.RSSI_UUID)?.getDescriptor(GameService.RSSI_SEND_REQUEST_UUID)
        //sendRequest?.value = "Device1".toByteArray(Charsets.UTF_8)
        //gatt?.writeDescriptor(sendRequest)

        val rssiCharacteristic = gatt?.getService(GameService.HOST_UUID)?.getCharacteristic(GameService.RSSI_UUID)
        rssiCharacteristic?.value = DeviceData(gatt!!.device.address, rssi.toFloat()).toByteArray() //ByteBuffer.allocate(4).putInt(_lastRssi).array()
        Log.d(TAG, "write " + rssiCharacteristic?.value?.size)
        gatt.writeCharacteristic(rssiCharacteristic)
    }
    private var _lastRssi = 0;
    */

    override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
        super.onCharacteristicRead(gatt, characteristic, status)
        when (characteristic?.uuid) {
            GameService.GAME_ID_UUID -> {
                val gameId = characteristic.value.toString(Charsets.UTF_8)
                Log.d(TAG, "callback: characteristic read successfully, gameId: $gameId")
                _gameViewModel.gameId = gameId
                gameJoinCallback.onGameJoin()
            }
        }
    }

    override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
        super.onDescriptorWrite(gatt, descriptor, status)

        // send characteristic
        //val rssiCharacteristic = gatt?.getService(GameService.SERVER_UUID)?.getCharacteristic(GameService.RSSI_UUID)
        //rssiCharacteristic?.value = ByteBuffer.allocate(4).putInt(_lastRssi).array()
        //gatt?.writeCharacteristic(rssiCharacteristic)
    }

    override fun onCharacteristicChanged(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?) {
        super.onCharacteristicChanged(gatt, characteristic)
        // TODO receive approved here
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
                    // declined
                }
            }
        }
    }

}