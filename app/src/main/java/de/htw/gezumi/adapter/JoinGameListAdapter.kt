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

            device.gameName.observe(lifecycleOwner, nameObserver)

            binding.textGameName.text = device.gameName.value

            val joinButton = binding.buttonJoin
            joinButton.setOnClickListener {
                listener.invoke(adapterPosition)
            }
            joinButton.isEnabled = _itemsEnabled

            // updates the view
            binding.invalidateAll()
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ItemJoinBinding.inflate(inflater)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ItemViewHolder, position: Int) {
        val device: Device = _hostDevices[position]
        viewHolder.bind(device)
    }

    override fun getItemCount(): Int {
        return _hostDevices.size
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        _recyclerView = recyclerView
    }

}