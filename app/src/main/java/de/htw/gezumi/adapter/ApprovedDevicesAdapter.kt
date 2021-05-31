package de.htw.gezumi.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import de.htw.gezumi.databinding.ItemApprovedDeviceBinding
import de.htw.gezumi.model.Device
import de.htw.gezumi.viewmodel.GameViewModel

class ApprovedDevicesAdapter(private val _devices: List<Device>) : RecyclerView.Adapter<ApprovedDevicesAdapter.ItemViewHolder>() {
    lateinit var lifecycleOwner: LifecycleOwner

    inner class ItemViewHolder(private val _binding: ItemApprovedDeviceBinding): RecyclerView.ViewHolder(_binding.root) {

        @kotlin.ExperimentalUnsignedTypes
        fun bind(device: Device) {
            // TODO move to item_bt_device per data binding
            //_binding.deviceName.text = GameViewModel.instance.game.getPlayer(device.deviceId)!!.name.value
            // Create the observer which updates the UI.
            val nameObserver = Observer<String> { newName ->
                // Update the UI, in this case, a TextView.
                _binding.deviceName.text = newName
            }

            // Observe the LiveData, passing in this activity as the LifecycleOwner and the observer.
            val liveName = GameViewModel.instance.game.getPlayer(device.deviceId)!!.name
            liveName.observe(lifecycleOwner, nameObserver)

            _binding.deviceName.text = liveName.value


            // make sure to include this so your view will be updated
            _binding.invalidateAll()
            _binding.executePendingBindings()
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