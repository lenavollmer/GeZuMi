package de.htw.gezumi

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import de.htw.gezumi.gatt.GattServer


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class JoinFragment : Fragment() {

    private val bluetoothGattServer: GattServer by activityViewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bluetoothGattServer.createViewModel(false)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_join, container, false)
    }

    override fun onStart() {
        super.onStart()
        bluetoothGattServer.startViewModel()
    }

    override fun onStop() {
        super.onStop()
        bluetoothGattServer.stopViewModel()
    }

    override fun onDestroy() {
        super.onDestroy()
        bluetoothGattServer.destroyViewModel()
    }
}