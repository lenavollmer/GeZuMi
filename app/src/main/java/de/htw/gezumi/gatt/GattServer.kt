package de.htw.gezumi.gatt

import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.ParcelUuid
import android.util.Log
import de.htw.gezumi.HostFragment
import de.htw.gezumi.controller.BluetoothController
import de.htw.gezumi.gatt.GameService

private const val TAG = "GattServer"

class GattServer(private val _context: Context, private val _bluetoothController: BluetoothController, private val _connectCallback : HostFragment.GattConnectCallback) {

    var bluetoothGattServer: BluetoothGattServer? = null
    private val _bluetoothManager = _context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    /* Collection of notification subscribers */
    private val _registeredDevices = mutableSetOf<BluetoothDevice>()

    /**
     * Listens for Bluetooth adapter events to enable/disable
     * advertising and server functionality.
     */
    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF)) {
                BluetoothAdapter.STATE_ON -> {
                    startAdvertising()
                    startServer()
                }
                BluetoothAdapter.STATE_OFF -> {
                    stopServer()
                    stopServer()
                }
            }
        }
    }

    /**
     * Callback to receive information about the advertisement process.
     */
    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            Log.i(TAG, "LE Advertise Started.")
        }

        override fun onStartFailure(errorCode: Int) {
            Log.w(TAG, "LE Advertise Failed: $errorCode")
        }
    }

    init {

        // Register for system Bluetooth events
        val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
        _context.registerReceiver(bluetoothReceiver, filter)
        if (!_bluetoothController.isBluetoothEnabled()) {
            Log.d(TAG, "Bluetooth is currently disabled...enabling")
            _bluetoothController.enableBluetooth()
        }
    }


    /**
     * Initialize the GATT server instance with the services/characteristics
     * from the Time Profile.
     */
    fun startServer() {
        Log.d(TAG, "start gatt server")
        bluetoothGattServer = _bluetoothManager.openGattServer(_context, GattServerCallback(_registeredDevices, this, _connectCallback))

        bluetoothGattServer?.addService(GameService.createGameService(GameService.SERVER_UUID))
            ?: Log.w(TAG, "Unable to create GATT server")
    }

    /**
     * Shut down the GATT server.
     */
    fun stopServer() {
        Log.d(TAG, "stop gatt server")
        bluetoothGattServer?.close()
    }

    /**
     * Begin advertising over Bluetooth that this device is connectable
     * and supports the Current Time Service.
     */
    fun startAdvertising() {
        val bluetoothLeAdvertiser: BluetoothLeAdvertiser? =
            _bluetoothManager.adapter.bluetoothLeAdvertiser

        bluetoothLeAdvertiser?.let {
            val settings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build()

            val data = AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(ParcelUuid(GameService.SERVER_UUID))
                .build()

            it.startAdvertising(settings, data, advertiseCallback)
        } ?: Log.w(TAG, "Failed to create advertiser")
        Log.d(TAG, "advertise started as host")
    }

    /**
     * Stop Bluetooth advertisements.
     */
    fun stopAdvertising() {
        Log.d(TAG, "stop advertising")
        val bluetoothLeAdvertiser: BluetoothLeAdvertiser? =
            _bluetoothManager.adapter.bluetoothLeAdvertiser
        bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback) ?: Log.w(TAG, "Failed to create advertiser")
    }

    /**
     * Send a time service notification to any devices that are subscribed
     * to the characteristic.
     */
    private fun notifyRegisteredDevices(timestamp: Long, adjustReason: Byte) {
        /*if (_registeredDevices.isEmpty()) {
            Log.i(TAG, "No subscribers registered")
            return
        }

        Log.i(TAG, "Sending update to ${_registeredDevices.size} subscribers")
        for (device in _registeredDevices) {
            val timeCharacteristic = bluetoothGattServer
                ?.getService(GameService.SERVER_UUID)
                ?.getCharacteristic(GameService.GAME_ID)
            timeCharacteristic?.value = someValue
            bluetoothGattServer?.notifyCharacteristicChanged(device, timeCharacteristic, false)
        }*/
    }

    fun stop() {
        if (_bluetoothManager.adapter.isEnabled) {
            stopServer()
            stopAdvertising()
        }
        _context.unregisterReceiver(bluetoothReceiver)
    }
}