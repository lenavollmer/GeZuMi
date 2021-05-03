package de.htw.gezumi.adapter

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import de.htw.gezumi.R
import de.htw.gezumi.databinding.ItemBtDeviceBinding
import de.htw.gezumi.databinding.ItemPlayerBinding

class PlayerDeviceListAdapter(private val _playerDevices: List<BluetoothDevice>) : RecyclerView.Adapter<PlayerDeviceListAdapter.ItemViewHolder>() {

    class ItemViewHolder(private val binding: ItemPlayerBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(device: BluetoothDevice) {
            // todo move to item_bt_device per data binding
            binding.textDeviceName.text = device.name
            // make sure to include this so your view will be updated
            binding.invalidateAll()
            binding.executePendingBindings()
        }
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemPlayerBinding.inflate(inflater)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        val device: BluetoothDevice = _playerDevices[position]
        holder.bind(device)
    }

    override fun getItemCount(): Int {
        return _playerDevices.size
    }

}
