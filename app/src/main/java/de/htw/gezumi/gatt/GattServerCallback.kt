package de.htw.gezumi.gatt

import android.bluetooth.*
import android.util.Log
import de.htw.gezumi.HostFragment
import de.htw.gezumi.Utils
import de.htw.gezumi.model.Device
import de.htw.gezumi.model.BluetoothData
import de.htw.gezumi.viewmodel.GameViewModel
import java.util.*

private const val TAG = "GattServerCallback"

class GattServerCallback(private val _gattServer: GattServer, private val _connectCallback : HostFragment.GattConnectCallback) : BluetoothGattServerCallback() {

    @kotlin.ExperimentalUnsignedTypes
    override fun onConnectionStateChange(bluetoothDevice: BluetoothDevice, status: Int, newState: Int) {
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            Log.d(TAG, "bluetoothDevice CONNECTED: $bluetoothDevice")
            // do nothing -> wait for join name
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            Log.d(TAG, "bluetoothDevice DISCONNECTED: $bluetoothDevice")
            _connectCallback.onGattDisconnect(bluetoothDevice)
            _gattServer.notifyGameEnding()
            GameViewModel.instance.onGameLeave()
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
                    GameService.randomIdPart
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
            GameService.JOIN_NAME_UUID -> {
                // called when a player wants to join
                val joinName: String? = if (value!!.isNotEmpty()) value.toString(Charsets.UTF_8) else null
                _connectCallback.onJoinRequest(device!!, joinName)
            }
            GameService.PLAYER_UPDATE_UUID -> {
                val deviceData = BluetoothData.fromBytes(value!!)
                Log.d(TAG, "received player update from: ${Utils.toHexString(deviceData.senderId)} device: ${Utils.toHexString(deviceData.id)} values=${deviceData.values.contentToString()}, size=${value.size}")
                GameViewModel.instance.playerUpdateCallback.onPlayerUpdate(deviceData)
            }
            GameService.PLAYER_IDENTIFICATION_UUID -> {
                // set active bluetooth device of device
                val deviceId = value!!
                Log.d(TAG, "received device identification: ${Utils.toHexString(deviceId)} bluetooth address: ${device!!.address}")
                val mDevice = Utils.findDevice(GameViewModel.instance.devices, deviceId)
                // if not known yet, add new device
                if (mDevice == null)
                    GameViewModel.instance.addDevice(Device(deviceId, 0, device))
                else // otherwise set bluetooth device to the one, that is connected over the gatt
                    mDevice.bluetoothDevice = device
            }
        }
        if (responseNeeded) {
            _gattServer.bluetoothGattServer?.sendResponse(
                device,
                requestId,
                BluetoothGatt.GATT_SUCCESS,
                0,
                ByteArray(0))
        }
    }

    override fun onNotificationSent(device: BluetoothDevice?, status: Int) {
        super.onNotificationSent(device, status)
        if (status == BluetoothGatt.GATT_SUCCESS) {
            Log.d(TAG, "indication sent")
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
                if (Arrays.equals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE, value)) {
                    Log.d(TAG, "subscribe device to indications: $device")
                        _gattServer.subscribedDevices.add(device)
                } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {
                    Log.d(TAG, "unsubscribe device from indi-/notifications: $device")
                    _gattServer.subscribedDevices.remove(device)
                }
                if (responseNeeded) {
                    _gattServer.bluetoothGattServer?.sendResponse(
                        device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE
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