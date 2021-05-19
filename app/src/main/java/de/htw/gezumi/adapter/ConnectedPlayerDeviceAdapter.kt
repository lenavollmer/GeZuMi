package de.htw.gezumi.adapter

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.htw.gezumi.databinding.BottomSheetItemBinding

class ConnectedPlayerDeviceAdapter(private val _btDevices: List<BluetoothDevice>, private val listener: (position: Int, status: STATUS) -> Unit) : RecyclerView.Adapter<ConnectedPlayerDeviceAdapter.ItemViewHolder>() {

    enum class STATUS {
        APPROVED, DECLINED
    }

    inner class ItemViewHolder(private val binding: BottomSheetItemBinding): RecyclerView.ViewHolder(binding.root) {

        fun bind(device: BluetoothDevice) {
            binding.deviceName.text = device.address


            val approveButton = binding.approve
            approveButton.setOnClickListener {
                listener.invoke(adapterPosition, STATUS.APPROVED)
            }

            val declineButton = binding.decline
            declineButton.setOnClickListener {
                listener.invoke(adapterPosition, STATUS.DECLINED)
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
        val binding = BottomSheetItemBinding.inflate(inflater)
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