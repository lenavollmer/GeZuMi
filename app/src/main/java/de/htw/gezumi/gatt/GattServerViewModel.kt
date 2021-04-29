package de.htw.gezumi.gatt

import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.*
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.ParcelUuid
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import java.util.*


private const val TAG = "GattServer"

// todo maybe refactor to service
class GattServer(application: Application) : AndroidViewModel(application) {

    @SuppressLint("StaticFieldLeak") // the fuck is das?
    private val context = getApplication<Application>().applicationContext

    /* Bluetooth API */
    private var _bluetoothManager: BluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager

    private var _bluetoothGattServer: BluetoothGattServer? = null

    /* Collection of notification subscribers */
    // todo set to livedata and make accessable
    private val _registeredDevices = mutableSetOf<BluetoothDevice>()

    // A variable to help us not setup twice
    private var _viewModelSetup = false


    // Called in the activity's onCreate(). Checks if it has been called before, and if not, sets up the data.
    // Returns true if everything went okay, or false if there was an error and therefore the activity should finish.
    fun createViewModel(): Boolean {
        // Check we haven't already been called
        if (!_viewModelSetup) {
            _viewModelSetup = true

            checkBluetoothSupport(_bluetoothManager.adapter)

            // Register for system Bluetooth events
            val filter = IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED)
            context.registerReceiver(bluetoothReceiver, filter)
            if (!_bluetoothManager.adapter.isEnabled) {
                Log.d(TAG, "Bluetooth is currently disabled...enabling")
                _bluetoothManager.adapter.enable()
            } else {
                Log.d(TAG, "Bluetooth enabled...starting services")
                startAdvertising()
                startServer()
            }
        }
        return true
    }

    fun startViewModel(): Boolean {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_TIME_TICK)
            addAction(Intent.ACTION_TIME_CHANGED)
            addAction(Intent.ACTION_TIMEZONE_CHANGED)
        }

        context.registerReceiver(timeReceiver, filter)

        return true
    }

    fun stopViewModel(): Boolean {
        context.unregisterReceiver(timeReceiver)
        return true
    }

    fun destroyViewModel() : Boolean {
        if (_bluetoothManager.adapter.isEnabled) {
            stopServer()
            stopAdvertising()
        }
        context.unregisterReceiver(bluetoothReceiver)
        return true
    }

    private val gattServerCallback = object : BluetoothGattServerCallback() {

        override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "BluetoothDevice CONNECTED: $device")
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                Log.i(TAG, "BluetoothDevice DISCONNECTED: $device")
                //Remove device from any active subscriptions
                _registeredDevices.remove(device)
            }
        }

        override fun onCharacteristicReadRequest(device: BluetoothDevice, requestId: Int, offset: Int,
                                                 characteristic: BluetoothGattCharacteristic
        ) {
            val now = System.currentTimeMillis()
            when {
                TimeProfile.CURRENT_TIME == characteristic.uuid -> {
                    Log.i(TAG, "Read CurrentTime")
                    _bluetoothGattServer?.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        TimeProfile.getExactTime(now, TimeProfile.ADJUST_NONE))
                }
                TimeProfile.LOCAL_TIME_INFO == characteristic.uuid -> {
                    Log.i(TAG, "Read LocalTimeInfo")
                    _bluetoothGattServer?.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        TimeProfile.getLocalTimeInfo(now))
                }
                else -> {
                    // Invalid characteristic
                    Log.w(TAG, "Invalid Characteristic Read: " + characteristic.uuid)
                    _bluetoothGattServer?.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null)
                }
            }
        }

        override fun onDescriptorReadRequest(device: BluetoothDevice, requestId: Int, offset: Int,
                                             descriptor: BluetoothGattDescriptor
        ) {
            if (TimeProfile.CLIENT_CONFIG == descriptor.uuid) {
                Log.d(TAG, "Config descriptor read")
                val returnValue = if (_registeredDevices.contains(device)) {
                    BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                } else {
                    BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE
                }
                _bluetoothGattServer?.sendResponse(device,
                    requestId,
                    BluetoothGatt.GATT_SUCCESS,
                    0,
                    returnValue)
            } else {
                Log.w(TAG, "Unknown descriptor read request")
                _bluetoothGattServer?.sendResponse(device,
                    requestId,
                    BluetoothGatt.GATT_FAILURE,
                    0, null)
            }
        }

        override fun onDescriptorWriteRequest(device: BluetoothDevice, requestId: Int,
                                              descriptor: BluetoothGattDescriptor,
                                              preparedWrite: Boolean, responseNeeded: Boolean,
                                              offset: Int, value: ByteArray) {
            if (TimeProfile.CLIENT_CONFIG == descriptor.uuid) {
                if (Arrays.equals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE, value)) {
                    Log.d(TAG, "Subscribe device to notifications: $device")
                    _registeredDevices.add(device)
                } else if (Arrays.equals(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE, value)) {
                    Log.d(TAG, "Unsubscribe device from notifications: $device")
                    _registeredDevices.remove(device)
                }

                if (responseNeeded) {
                    _bluetoothGattServer?.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0, null)
                }
            } else {
                Log.w(TAG, "Unknown descriptor write request")
                if (responseNeeded) {
                    _bluetoothGattServer?.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0, null)
                }
            }
        }
    }

    /**
     * Listens for system time changes and triggers a notification to
     * Bluetooth subscribers.
     */
    private val timeReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val adjustReason = when (intent.action) {
                Intent.ACTION_TIME_CHANGED -> TimeProfile.ADJUST_MANUAL
                Intent.ACTION_TIMEZONE_CHANGED -> TimeProfile.ADJUST_TIMEZONE
                Intent.ACTION_TIME_TICK -> TimeProfile.ADJUST_NONE
                else -> TimeProfile.ADJUST_NONE
            }
            val now = System.currentTimeMillis()
            notifyRegisteredDevices(now, adjustReason)
        }
    }

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
                    stopAdvertising()
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

    /**
     * Initialize the GATT server instance with the services/characteristics
     * from the Time Profile.
     */
    private fun startServer() {
        _bluetoothGattServer = _bluetoothManager.openGattServer(context, gattServerCallback)

        _bluetoothGattServer?.addService(TimeProfile.createTimeService())
            ?: Log.w(TAG, "Unable to create GATT server")
    }

    /**
     * Shut down the GATT server.
     */
    private fun stopServer() {
        _bluetoothGattServer?.close()
    }

    /**
    * Begin advertising over Bluetooth that this device is connectable
    * and supports the Current Time Service.
    */
    private fun startAdvertising() {
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
                .addServiceUuid(ParcelUuid(TimeProfile.TIME_SERVICE))
                .build()

            it.startAdvertising(settings, data, advertiseCallback)
        } ?: Log.w(TAG, "Failed to create advertiser")
    }

    /**
     * Stop Bluetooth advertisements.
     */
    private fun stopAdvertising() {
        val bluetoothLeAdvertiser: BluetoothLeAdvertiser? =
            _bluetoothManager.adapter.bluetoothLeAdvertiser
        bluetoothLeAdvertiser?.stopAdvertising(advertiseCallback) ?: Log.w(TAG, "Failed to create advertiser")
    }

    /**
     * Verify the level of Bluetooth support provided by the hardware.
     * @return true if Bluetooth is properly supported, false otherwise.
     */
    private fun checkBluetoothSupport(adapter: BluetoothAdapter?): Boolean {

        if (adapter == null) {
            Log.w(TAG, "Bluetooth is not supported")
            return false
        }

        if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Log.w(TAG, "Bluetooth LE is not supported")
            return false
        }
        return true
    }

    /**
     * Send a time service notification to any devices that are subscribed
     * to the characteristic.
     */
    private fun notifyRegisteredDevices(timestamp: Long, adjustReason: Byte) {
        if (_registeredDevices.isEmpty()) {
            Log.i(TAG, "No subscribers registered")
            return
        }
        val exactTime = TimeProfile.getExactTime(timestamp, adjustReason)

        Log.i(TAG, "Sending update to ${_registeredDevices.size} subscribers")
        for (device in _registeredDevices) {
            val timeCharacteristic = _bluetoothGattServer
                ?.getService(TimeProfile.TIME_SERVICE)
                ?.getCharacteristic(TimeProfile.CURRENT_TIME)
            timeCharacteristic?.value = exactTime
            _bluetoothGattServer?.notifyCharacteristicChanged(device, timeCharacteristic, false)
        }
    }
}