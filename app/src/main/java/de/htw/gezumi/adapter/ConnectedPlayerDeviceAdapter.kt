package de.htw.gezumi.adapter

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import de.htw.gezumi.databinding.BottomSheetItemBinding

class ConnectedPlayerDeviceAdapter(private val _playerNames: List<String>, private val listener: (position: Int, status: STATUS) -> Unit) : RecyclerView.Adapter<ConnectedPlayerDeviceAdapter.ItemViewHolder>() {

    enum class STATUS {
        APPROVED, DECLINED
    }

    inner class ItemViewHolder(private val binding: BottomSheetItemBinding): RecyclerView.ViewHolder(binding.root) {

        fun bind(playerName: String) {
            binding.deviceName.text = playerName


            val approveButton = binding.approve
            approveButton.setOnClickListener {
                listener.invoke(adapterPosition, STATUS.APPROVED)
            }

            val declineButton = binding.decline
            declineButton.setOnClickListener {
                listener.invoke(adapterPosition, STATUS.DECLINED)
            }
            // updates the view
            binding.invalidateAll()
            binding.executePendingBindings()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = BottomSheetItemBinding.inflate(inflater)
        return ItemViewHolder(binding)
    }

    override fun onBindViewHolder(viewHolder: ItemViewHolder, position: Int) {
        viewHolder.bind(_playerNames[position])
    }

    override fun getItemCount(): Int {
        return _playerNames.size
    }
}