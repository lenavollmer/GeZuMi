package de.htw.gezumi.gatt

import android.bluetooth.*
import android.util.Log
import androidx.fragment.app.activityViewModels
import de.htw.gezumi.HostFragment
import de.htw.gezumi.model.DeviceData
import de.htw.gezumi.viewmodel.GameViewModel
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
                    GameService.randomIdPart + GameService.gameName.toByteArray(Charsets.UTF_8)
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

    @kotlin.ExperimentalUnsignedTypes
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
            GameService.PLAYER_UPDATE_UUID -> {
                val deviceData = DeviceData.fromBytes(value!!)
                Log.d(TAG, "player update: device: ${deviceData.deviceAddress} values=${deviceData.values.contentToString()}, size=${value.size}")
                // TODO: do something with the received data (use for calculations)
                _gattServer.notifyHostUpdate(deviceData) // for test
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