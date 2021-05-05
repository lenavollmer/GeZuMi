package de.htw.gezumi.gatt

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.util.Log
import de.htw.gezumi.gatt.GameService
import de.htw.gezumi.model.DeviceViewModel
import java.util.*

private const val TAG = "ClientGattCallback"

class GattClientCallback(private val _deviceViewModel: DeviceViewModel) : BluetoothGattCallback() {

    private var _rssiTimer = Timer() // TODO timer just for test purposes here, rssi value doesn't have to do with gatt connection

    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            Log.d(TAG, "callback: connected")
            Log.d(TAG, "discover services")
            val success2 = gatt?.discoverServices();
            Log.d(TAG, "discover services: $success2")

        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            _rssiTimer.cancel()
        }
    }

    override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
        super.onReadRemoteRssi(gatt, rssi, status)
        _deviceViewModel.addRSSI(rssi)
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        super.onServicesDiscovered(gatt, status)
        Log.d(TAG, "services discovered")
        Log.d(TAG, "read game id")
        val gameIdCharacteristic = gatt?.getService(GameService.SERVER_UUID)?.getCharacteristic(GameService.GAME_ID)
        val success = gatt?.readCharacteristic(gameIdCharacteristic)
        Log.d(TAG, "read game id: $success")

        Log.d(TAG, "start testing rssi")
        val task: TimerTask = object : TimerTask() {
                override fun run() {
                    gatt?.readRemoteRssi()
                }
            }
            _rssiTimer.schedule(task, 1000, 1000)
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        super.onCharacteristicRead(gatt, characteristic, status)
        if (characteristic?.uuid == GameService.GAME_ID) {
            val gameId = characteristic?.value?.toString(Charsets.UTF_8)
            Log.d(TAG, "callback: characteristic read successfully, gameId: $gameId")
        }
    }
}