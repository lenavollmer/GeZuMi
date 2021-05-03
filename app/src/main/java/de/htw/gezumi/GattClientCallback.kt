package de.htw.gezumi

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothProfile
import android.util.Log
import androidx.fragment.app.viewModels
import de.htw.gezumi.gatt.TimeProfile
import de.htw.gezumi.model.DeviceViewModel

private const val TAG = "ClientGattCallback"

class GattClientCallback(private val _deviceViewModel: DeviceViewModel) : BluetoothGattCallback() {

    override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            Log.d(TAG, "callback: connected")
            Log.d(TAG, "discover services")
            val success2 = gatt?.discoverServices();
            Log.d(TAG, "discover services: $success2")
            /*val task: TimerTask = object : TimerTask() {
                override fun run() {
                    gatt?.readRemoteRssi()
                }
            }
            _rssiTimer.schedule(task, 1000, 1000)*/
        } /*else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                _rssiTimer.cancel()
            }*/
    }

    override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
        super.onReadRemoteRssi(gatt, rssi, status)
        _deviceViewModel.addRSSI(rssi)
    }

    override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
        super.onServicesDiscovered(gatt, status)
        Log.d(TAG, "services discovered")
        Log.d(TAG, "read game id")
        val gameIdCharacteristic = gatt?.getService(TimeProfile.SERVER_UUID)?.getCharacteristic(TimeProfile.GAME_ID)
        val success = gatt?.readCharacteristic(gameIdCharacteristic)
        Log.d(TAG, "read game id: $success")
    }

    override fun onCharacteristicRead(
        gatt: BluetoothGatt?,
        characteristic: BluetoothGattCharacteristic?,
        status: Int
    ) {
        super.onCharacteristicRead(gatt, characteristic, status)
        if (characteristic?.uuid == TimeProfile.GAME_ID) {
            val gameId = characteristic?.value?.toString(Charsets.UTF_8)
            Log.d(TAG, "callback: characteristic read successfully, gameId: $gameId")
        }
    }
}