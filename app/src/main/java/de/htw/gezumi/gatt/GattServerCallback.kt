package de.htw.gezumi.gatt

import android.bluetooth.*
import de.htw.gezumi.HostFragment
import de.htw.gezumi.Utils
import de.htw.gezumi.model.Device
import de.htw.gezumi.model.BluetoothData
import de.htw.gezumi.viewmodel.GameViewModel
import java.util.*

class GattServerCallback(private val _gattServer: GattServer, private val _connectCallback : HostFragment.GattConnectCallback) : BluetoothGattServerCallback() {

    @kotlin.ExperimentalUnsignedTypes
    override fun onConnectionStateChange(bluetoothDevice: BluetoothDevice, status: Int, newState: Int) {
        if (newState == BluetoothProfile.STATE_CONNECTED) {
            // do nothing -> wait for join name
        } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
            _connectCallback.onGattDisconnect(bluetoothDevice)
        }
    }

    override fun onCharacteristicReadRequest(
        device: BluetoothDevice, requestId: Int, offset: Int,
        characteristic: BluetoothGattCharacteristic
    ) {
        when (characteristic.uuid) {
            GameService.GAME_ID_UUID -> {
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
                GameViewModel.instance.playerUpdateCallback.onPlayerUpdate(deviceData)
            }
            GameService.PLAYER_IDENTIFICATION_UUID -> {
                // set active bluetooth device of device
                val deviceId = value!!
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

    override fun onDescriptorWriteRequest(
        device: BluetoothDevice, requestId: Int,
        descriptor: BluetoothGattDescriptor,
        preparedWrite: Boolean, responseNeeded: Boolean,
        offset: Int, value: ByteArray
    ) {
        when (descriptor.uuid) {
            GameService.CLIENT_CONFIG -> {
                if (Arrays.equals(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE, value)) {
                    _gattServer.subscribedDevices.add(device)
                } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {
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