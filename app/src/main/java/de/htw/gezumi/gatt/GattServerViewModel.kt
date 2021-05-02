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

// https://github.com/androidthings/sample-bluetooth-le-gattserver
private const val TAG = "GattServer"

// todo maybe refactor to service
class GattServer(application: Application) : AndroidViewModel(application) {




    // A variable to help us not setup twice
    private var _viewModelSetup = false


/*
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

    fun destroyViewModel(): Boolean {
        if (_bluetoothManager.adapter.isEnabled) {
            stopServer()
            stopAdvertising()
        }
        context.unregisterReceiver(bluetoothReceiver)
        return true
    }

*/


}