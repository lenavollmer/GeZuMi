package de.htw.gezumi

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import java.util.*


val MY_SERVICE: UUID = UUID.fromString("e0ec8d9c-5e4d-470a-b87f-64f433685301")
val MY_CHARACTERISTIC: UUID = UUID.fromString("e0ec8d9c-5e4d-470a-b87f-64f433685302")

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class JoinFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_join, container, false)
    }

    /**
     * Return a configured [BluetoothGattService] instance for the
     * Custom Service.
     */
    fun createCustomBleService(): BluetoothGattService {
        val service = BluetoothGattService(
            MY_SERVICE,
            BluetoothGattService.SERVICE_TYPE_PRIMARY
        )

        // Current Configuration characteristic
        val characteristic = BluetoothGattCharacteristic(
            MY_CHARACTERISTIC,
            BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_READ or BluetoothGattCharacteristic.PERMISSION_WRITE
        )
        val serviceAdded: Boolean = service.addCharacteristic(characteristic)
        print(serviceAdded)
        return service
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

//        view.findViewById<Button>(R.id.button_host).setOnClickListener {
//            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
//        }
    }
}