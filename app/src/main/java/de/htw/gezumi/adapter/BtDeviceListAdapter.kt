package de.htw.gezumi.adapter

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import de.htw.gezumi.R
import de.htw.gezumi.databinding.ItemBtDeviceBinding

class BtDeviceListAdapter(private val _btDevices: List<BluetoothDevice>) : RecyclerView.Adapter<BtDeviceListAdapter.ItemViewHolder>() {

    class ItemViewHolder(private val binding: ItemBtDeviceBinding): RecyclerView.ViewHolder(binding.root) {
        fun bind(device: BluetoothDevice) {
            // todo move to item_bt_device per data binding
            binding.contactName.text = device.name
            val button = binding.messageButton
            button.setText(R.string.connect)
            button.setOnClickListener {

                itemView.findNavController().navigate(R.id.action_ClientFragment_to_Game, bundleOf("hostDevice" to device))
            }
            // make sure to include this so your view will be updated
            binding.invalidateAll()
            binding.executePendingBindings()
        }
    }

    // ... constructor and member variables
    // Usually involves inflating a layout from XML and returning the holder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemBtDeviceBinding.inflate(inflater)
        return ItemViewHolder(binding)
    }

    // Involves populating data into the item through holder
    override fun onBindViewHolder(viewHolder: ItemViewHolder, position: Int) {
        // Get the data model based on position
        val device: BluetoothDevice = _btDevices[position]
        viewHolder.bind(device)
    }

    // Returns the total count of items in the list
    override fun getItemCount(): Int {
        return _btDevices.size
    }
}