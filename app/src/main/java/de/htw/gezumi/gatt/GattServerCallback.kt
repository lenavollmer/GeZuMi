package de.htw.gezumi.gatt

import android.bluetooth.*
import android.util.Log
import de.htw.gezumi.HostFragment
import de.htw.gezumi.model.DeviceData
import java.util.*

private const val TAG = "GattServerCallback"

class GattServerCallback(private val _subscribedDevices: MutableSet<BluetoothDevice>, private val _gattServer: GattServer, private val _connectCallback : HostFragment.GattConnectCallback) : BluetoothGattServerCallback() {

    override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            Log.d(TAG, "bluetoothDevice CONNECTED: $device")
            _connectCallback.onGattConnect(device)
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            Log.d(TAG, "bluetoothDevice DISCONNECTED: $device")
            //Remove device from any active subscriptions
            _connectCallback.onGattDisconnect(device)
            _subscribedDevices.remove(device)
        }
    }

    override fun onCharacteristicReadRequest(
        device: BluetoothDevice, requestId: Int, offset: Int,
        characteristic: BluetoothGattCharacteristic
    ) {
        when (characteristic.uuid) {
            GameService.GAME_ID_UUID -> {
                Log.d(TAG, "read game ID ${device.address}")
                _gattServer.bluetoothGattServer?.sendResponse(
                    device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    0,
                    GameService.gameIdPostfix.toByteArray(Charsets.UTF_8)
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

    override fun onCharacteristicWriteRequest(
        device: BluetoothDevice?,
        requestId: Int,
        characteristic: BluetoothGattCharacteristic?,
        preparedWrite: Boolean,
        responseNeeded: Boolean,
        offset: Int,
        value: ByteArray?
    ) {
        super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded,
            offset, value)
        when (characteristic?.uuid) {
            GameService.RSSI_UUID -> {
                Log.d(TAG, "read " + value?.size)
                val deviceData = DeviceData.fromBytes(value!!)
                Log.d(TAG, "write received: device: ${deviceData.deviceAddress} rssi = ${deviceData.value}")
            }
        }
    }

    override fun onDescriptorWriteRequest(
        device: BluetoothDevice, requestId: Int,
        descriptor: BluetoothGattDescriptor,
        preparedWrite: Boolean, responseNeeded: Boolean,
        offset: Int, value: ByteArray
    ) {
        when (descriptor.uuid) {
            GameService.CLIENT_CONFIG -> {
                if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
                    Log.d(TAG, "subscribe device to notifications: $device")
                    _subscribedDevices.add(device)
                } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {
                    Log.d(TAG, "unsubscribe device from notifications: $device")
                    _subscribedDevices.remove(device)
                }
                if (responseNeeded) {
                    _gattServer.bluetoothGattServer?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                    )
                }
            }
            else -> {
                Log.d(TAG, "Unknown descriptor write request")
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
}