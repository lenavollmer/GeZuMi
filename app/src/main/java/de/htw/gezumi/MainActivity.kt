package de.htw.gezumi

import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import kotlin.system.exitProcess

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        this.registerReceiver(_btReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
    }

    var resultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) {
            Toast.makeText(
                this,
                getString(R.string.bluetooth_not_enabled),
                Toast.LENGTH_LONG
            ).show()
            setContentView(R.layout.activity_main)
            exitProcess(0)
        }
    }

    private val _btReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action: String? = intent.action

            // It means the user has changed his bluetooth state.
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                if (BluetoothAdapter.getDefaultAdapter().state == BluetoothAdapter.STATE_OFF) {
                    resultLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (BluetoothAdapter.getDefaultAdapter().state == BluetoothAdapter.STATE_OFF) {
            resultLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
        }
    }
}