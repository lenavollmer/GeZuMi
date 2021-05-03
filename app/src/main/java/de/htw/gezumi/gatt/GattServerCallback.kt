package de.htw.gezumi.gatt

import android.bluetooth.*
import android.util.Log
import de.htw.gezumi.HostFragment
import de.htw.gezumi.gatt.GameService
import java.util.*

private const val TAG = "GattServerCallback"

class GattServerCallback(private val _registeredDevices: MutableSet<BluetoothDevice>, private val _gattServer: GattServer, private val _connectCallback : HostFragment.GattConnectCallback) : BluetoothGattServerCallback() {

    override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            Log.d(TAG, "BluetoothDevice CONNECTED: $device")
            _connectCallback.onGattConnect(device)
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            Log.d(TAG, "BluetoothDevice DISCONNECTED: $device")
            //Remove device from any active subscriptions
            _connectCallback.onGattDisconnect(device)
            _registeredDevices.remove(device)
        }
    }

    override fun onCharacteristicReadRequest(
        device: BluetoothDevice, requestId: Int, offset: Int,
        characteristic: BluetoothGattCharacteristic
    ) {
        when (characteristic.uuid) {
            GameService.GAME_ID -> {
                Log.d(TAG, "read game ID")
                _gattServer.bluetoothGattServer?.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    0,
                    GameService.getGameId()
                )
            }
            else -> {
                // Invalid characteristic
                Log.d(TAG, "Invalid Characteristic Read: " + characteristic.uuid)
                _gattServer.bluetoothGattServer?.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_FAILURE,
                    0,
                    null
                )
            }
        }
    }

    override fun onDescriptorReadRequest(
        device: BluetoothDevice, requestId: Int, offset: Int,
        descriptor: BluetoothGattDescriptor
    ) {
        if (GameService.CLIENT_CONFIG == descriptor.uuid) {
            Log.d(TAG, "Config descriptor read")
            val returnValue = if (_registeredDevices.contains(device)) {
                BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
            } else {
                BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
            }
            _gattServer.bluetoothGattServer?.sendResponse(
                device,
                requestId,
                BluetoothGatt.GATT_SUCCESS,
                0,
                returnValue
            )
        } else {
            Log.w(TAG, "Unknown descriptor read request")
            _gattServer.bluetoothGattServer?.sendResponse(
                device,
                requestId,
                BluetoothGatt.GATT_FAILURE,
                0, null
            )
        }
    }

    override fun onDescriptorWriteRequest(
        device: BluetoothDevice, requestId: Int,
        descriptor: BluetoothGattDescriptor,
        preparedWrite: Boolean, responseNeeded: Boolean,
        offset: Int, value: ByteArray
    ) {
        if (GameService.CLIENT_CONFIG == descriptor.uuid) {
            if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
                Log.d(TAG, "Subscribe device to notifications: $device")
                _registeredDevices.add(device)
            } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {
                Log.d(TAG, "Unsubscribe device from notifications: $device")
                _registeredDevices.remove(device)
            }

            if (responseNeeded) {
                _gattServer.bluetoothGattServer?.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    0, null
                )
            }
        } else {
            Log.w(TAG, "Unknown descriptor write request")
            if (responseNeeded) {
                _gattServer.bluetoothGattServer?.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_FAILURE,
                    0, null
                )
            }
        }
    }
}