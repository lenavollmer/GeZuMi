package de.htw.gezumi

import android.bluetooth.BluetoothDevice
import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import de.htw.gezumi.databinding.FragmentGameBinding
import de.htw.gezumi.util.DistanceCalculationModel


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class GameFragment : Fragment() {

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

        binding?.textView?.text = currentDevice.name
    }
}