package de.htw.gezumi.gatt

import android.bluetooth.*
import android.util.Log
import de.htw.gezumi.model.DeviceViewModel
import java.nio.ByteBuffer
import java.util.*

private const val TAG = "ClientGattCallback"

class GattClientCallback(private val _deviceViewModel: DeviceViewModel) : BluetoothGattCallback() {

    private var _rssiTimer = Timer() // TODO timer just for test purposes here, rssi value doesn't have to do with gatt connection

    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            Log.d(TAG, "callback: connected")
            Log.d(TAG, "discover services")
            gatt?.discoverServices();

        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            _rssiTimer.cancel()
        }
    }

    override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
        super.onReadRemoteRssi(gatt, rssi, status)
        _deviceViewModel.addRSSI(rssi)
        // TODO the raw rssi value is transferred here, but a processed value should
        _lastRssi = rssi
        // write device the rssi was measured for (in this test case it's just the host)
        val rssiDevice = gatt?.getService(GameService.SERVER_UUID)?.getCharacteristic(GameService.RSSI_UUID)?.getDescriptor(GameService.RSSI_DEVICE_UUID)
        rssiDevice?.value = "Device1".toByteArray(Charsets.UTF_8)
        gatt?.writeDescriptor(rssiDevice)
    }
    private var _lastRssi = 0;
    override fun onDescriptorWrite(gatt: BluetoothGatt?, descriptor: BluetoothGattDescriptor?, status: Int) {
        super.onDescriptorWrite(gatt, descriptor, status)

        // send characteristic
        val rssiCharacteristic = gatt?.getService(GameService.SERVER_UUID)?.getCharacteristic(GameService.RSSI_UUID)
        rssiCharacteristic?.value = ByteBuffer.allocate(4).putInt(_lastRssi).array()
        gatt?.writeCharacteristic(rssiCharacteristic)
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        super.onServicesDiscovered(gatt, status)
        Log.d(TAG, "services discovered")
        Log.d(TAG, "read game id")
        val gameIdCharacteristic = gatt?.getService(GameService.SERVER_UUID)?.getCharacteristic(GameService.GAME_ID_UUID)
        gatt?.readCharacteristic(gameIdCharacteristic)

        Log.d(TAG, "start testing rssi")
        val task: TimerTask = object : TimerTask() {
                override fun run() {
                    gatt?.readRemoteRssi()
                }
            }
            _rssiTimer.schedule(task, 1000, 1000)
    }

    override fun onCharacteristicRead(gatt: BluetoothGatt?, characteristic: BluetoothGattCharacteristic?, status: Int) {
        super.onCharacteristicRead(gatt, characteristic, status)
        when (characteristic?.uuid) {
            GameService.GAME_ID_UUID -> {
                val gameId = characteristic?.value?.toString(Charsets.UTF_8)
                Log.d(TAG, "callback: characteristic read successfully, gameId: $gameId")
            }
        }
    }
}