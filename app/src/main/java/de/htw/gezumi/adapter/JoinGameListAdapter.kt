package de.htw.gezumi.adapter

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.htw.gezumi.databinding.ItemJoinBinding


class JoinGameListAdapter(private val _btDevices: List<BluetoothDevice>, private val listener: (position: Int) -> Unit) : RecyclerView.Adapter<JoinGameListAdapter.ItemViewHolder>() {

    inner class ItemViewHolder(private val binding: ItemJoinBinding): RecyclerView.ViewHolder(binding.root) {

        fun bind(device: BluetoothDevice) {
            binding.deviceName.text = device.name.toString()


            val approveButton = binding.buttonJoin
            approveButton.setOnClickListener {
                listener.invoke(adapterPosition)
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
        val binding = ItemJoinBinding.inflate(inflater)
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