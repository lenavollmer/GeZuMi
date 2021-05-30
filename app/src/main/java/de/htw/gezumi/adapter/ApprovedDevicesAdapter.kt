package de.htw.gezumi.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.htw.gezumi.Utils
import de.htw.gezumi.databinding.ItemApprovedDeviceBinding
import de.htw.gezumi.model.Device
import de.htw.gezumi.viewmodel.GameViewModel

class ApprovedDevicesAdapter(private val _devices: List<Device>) : RecyclerView.Adapter<ApprovedDevicesAdapter.ItemViewHolder>() {

    class ItemViewHolder(private val binding: ItemApprovedDeviceBinding): RecyclerView.ViewHolder(binding.root) {

        @kotlin.ExperimentalUnsignedTypes
        fun bind(device: Device) {
            // TODO move to item_bt_device per data binding
            val playerName = GameViewModel.instance.game.getPlayer(device.deviceId)?.name
            if (playerName != null)
                binding.deviceName.text = playerName.value
            else
                binding.deviceName.text = Utils.toHexString(device.deviceId)
            // make sure to include this so your view will be updated
            binding.invalidateAll()
            binding.executePendingBindings()
        }
    }

    // ... constructor and member variables
    // Usually involves inflating a layout from XML and returning the holder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemApprovedDeviceBinding.inflate(inflater)
        return ItemViewHolder(binding)
    }

    @kotlin.ExperimentalUnsignedTypes
    override fun onBindViewHolder(viewHolder: ItemViewHolder, position: Int) {
        // Get the data model based on position
        val device: Device = _devices[position]
        viewHolder.bind(device)
    }

    // Returns the total count of items in the list
    override fun getItemCount(): Int {
        return _devices.size
    }
}