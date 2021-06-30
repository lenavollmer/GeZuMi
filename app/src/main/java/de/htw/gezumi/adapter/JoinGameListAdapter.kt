package de.htw.gezumi.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.RecyclerView
import de.htw.gezumi.databinding.ItemJoinBinding
import de.htw.gezumi.model.Device

class JoinGameListAdapter(private val _hostDevices: List<Device>, private val listener: (position: Int) -> Unit) : RecyclerView.Adapter<JoinGameListAdapter.ItemViewHolder>() {
    private lateinit var _recyclerView: RecyclerView
    lateinit var lifecycleOwner: LifecycleOwner
    private var _itemsEnabled = true

    inner class ItemViewHolder(val binding: ItemJoinBinding): RecyclerView.ViewHolder(binding.root) {

        fun bind(device: Device) {
            // Create the observer which updates the UI.
            val nameObserver = Observer<String> { newName ->
                // Update the UI, in this case, a TextView.
                binding.textGameName.text = newName
            }

            // Observe the LiveData, passing in this activity as the LifecycleOwner and the observer.
            device.gameName.observe(lifecycleOwner, nameObserver)

            binding.textGameName.text = device.gameName.value

            val joinButton = binding.buttonJoin
            joinButton.setOnClickListener {
                listener.invoke(adapterPosition)
            }
            joinButton.isEnabled = _itemsEnabled

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
        val device: Device = _hostDevices[position]
        viewHolder.bind(device)
    }

    // Returns the total count of items in the list
    override fun getItemCount(): Int {
        return _hostDevices.size
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        _recyclerView = recyclerView
    }

}