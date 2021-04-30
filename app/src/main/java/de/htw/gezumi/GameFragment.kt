package de.htw.gezumi

import android.bluetooth.*
import android.os.Bundle
import android.view.*
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import de.htw.gezumi.databinding.FragmentGameBinding
import de.htw.gezumi.util.DistanceCalculationModel
import java.util.*


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class GameFragment : Fragment() {
    private var mRssiTimer = Timer()
    private val distanceModel: DistanceCalculationModel by activityViewModels()

    private lateinit var gatt : BluetoothGatt
    private lateinit var currentDevice: BluetoothDevice
    private lateinit var binding: FragmentGameBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        currentDevice = arguments?.getParcelable("device")!!
        gatt = currentDevice.connectGatt(activity, false, gattCallback)
        gatt.connect()

        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_game, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.device.text = currentDevice.name
        binding.rssi.text = distanceModel.distance.toString()

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
                val task: TimerTask = object : TimerTask() {
                    override fun run() {
                        gatt?.readRemoteRssi()
                    }
                }
                mRssiTimer.schedule(task, 1000, 1000)
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mRssiTimer.cancel()
            }
        }
        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            super.onReadRemoteRssi(gatt, rssi, status)
            distanceModel.addRSSI(rssi)
        }
    }
}