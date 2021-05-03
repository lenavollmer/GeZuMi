package de.htw.gezumi

import android.bluetooth.*
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import de.htw.gezumi.databinding.FragmentGameBinding
import de.htw.gezumi.gatt.TimeProfile
import de.htw.gezumi.model.DeviceViewModel
import java.util.*

private const val TAG = "GameFragment"

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class GameFragment : Fragment() {

    private lateinit var binding: FragmentGameBinding
    private val deviceViewModel: DeviceViewModel by viewModels()

    private lateinit var gatt: BluetoothGatt
    private lateinit var currentDevice: BluetoothDevice
    private var _rssiTimer = Timer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        currentDevice = arguments?.getParcelable("device")!!
        gatt = currentDevice.connectGatt(activity, false, gattCallback)
        var success1 = gatt.connect()
        Log.d(TAG, "connected to gatt: $success1")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_game, container, false)
        deviceViewModel.setName(currentDevice.name)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.deviceViewModel = deviceViewModel
    }

    override fun onPause() {
        super.onPause()
        gatt.disconnect()
    }

    override fun onResume() {
        super.onResume()
        gatt.connect()
    }

    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
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
            deviceViewModel.addRSSI(rssi)
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
}