package de.htw.gezumi.adapter

import android.bluetooth.BluetoothDevice
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import de.htw.gezumi.R
import de.htw.gezumi.databinding.ItemBtDeviceBinding
import de.htw.gezumi.databinding.ItemPlayerBinding

private const val TAG = "PlayerDeviceLA"

class PlayerDeviceListAdapter(private val _playerDevices: List<BluetoothDevice>) : RecyclerView.Adapter<PlayerDeviceListAdapter.ItemViewHolder>() {

    class ItemViewHolder(private val _binding: ItemPlayerBinding): RecyclerView.ViewHolder(_binding.root) {
        fun bind(device: BluetoothDevice, position: Int) {
            Log.d(TAG, "bind: ${device.name}") // TODO: device name is null
            _binding.textDeviceName.text = "Player " + position + ": " + device.name
            // make sure to include this so your view will be updated
            _binding.invalidateAll()
            _binding.executePendingBindings()
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemPlayerBinding.inflate(inflater)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val device: BluetoothDevice = _playerDevices[position]
        holder.bind(device, position)
    }

    override fun getItemCount(): Int {
        return _playerDevices.size
    }

}
