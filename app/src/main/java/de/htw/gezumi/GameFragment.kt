package de.htw.gezumi

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import de.htw.gezumi.databinding.FragmentGameBinding
import de.htw.gezumi.util.DistanceCalculationModel


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class GameFragment : Fragment() {
    private val btAdapter = BluetoothAdapter.getDefaultAdapter()

    private val distanceModel: DistanceCalculationModel by activityViewModels()

    private lateinit var currentDevice : BluetoothDevice
    private var binding: FragmentGameBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        currentDevice = arguments?.getParcelable("device")!!


        val fragmentBinding = FragmentGameBinding.inflate(inflater, container, false)
        binding = fragmentBinding

        return fragmentBinding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentDevice.connectGatt(activity, true, gattCallback)


        binding?.textView?.text = currentDevice.name
    }

    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onReadRemoteRssi(gatt: BluetoothGatt?, rssi: Int, status: Int) {
            super.onReadRemoteRssi(gatt, rssi, status)
            Log.d("D/GameFragment", rssi.toString())
        }

    }
}