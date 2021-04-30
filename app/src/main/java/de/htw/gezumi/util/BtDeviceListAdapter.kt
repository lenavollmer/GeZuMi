package de.htw.gezumi.util

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import de.htw.gezumi.R
import de.htw.gezumi.databinding.ItemBtDeviceBinding


// Create the basic adapter extending from RecyclerView.Adapter
// Note that we specify the custom ViewHolder which gives us access to our views
/*
class BtDeviceListAdapter : RecyclerView.Adapter<BtDeviceListAdapter.ViewHolder>() {


    private val mBtDevices: ArrayList<BluetoothDevice> = ArrayList()

    fun addDevice(device: BluetoothDevice) {
        if (!mBtDevices.contains(device)) {
            mBtDevices.add(device)
        }
    }

    fun getDevice(position: Int): BluetoothDevice {
        return mBtDevices[position]
    }

    fun clear() {
        mBtDevices.clear()
    }

    class ViewHolder private constructor(private val binding: ItemBtDeviceBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(device: BluetoothDevice) {

            binding.contactName.text = device.name
            val button = binding.messageButton
            button.setText(R.string.connect)
            button.setOnClickListener {
                itemView.findNavController().navigate(R.id.action_HostFragment_to_Game, bundleOf("device" to device))
            }
            // make sure to include this so your view will be updated
            binding.executePendingBindings()
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ItemBtDeviceBinding.inflate(layoutInflater, parent, false)

                return ViewHolder(binding)
            }
        }
    }

    // ... constructor and member variables
    // Usually involves inflating a layout from XML and returning the holder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    // Involves populating data into the item through holder
    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        // Get the data model based on position
        val device: BluetoothDevice = mBtDevices[position]
        viewHolder.bind(device)
    }

    // Returns the total count of items in the list
    override fun getItemCount(): Int {
        return mBtDevices.size
    }
}
*/

class BtDeviceListAdapter() : RecyclerView.Adapter<BtDeviceListAdapter.MyViewHolder>() {

    private val items: ArrayList<BluetoothDevice> = ArrayList()

    inner class MyViewHolder(b: ItemBtDeviceBinding) : RecyclerView.ViewHolder(b.root) {
        var binding: ItemBtDeviceBinding = b

    }

    fun addDevice(device: BluetoothDevice) {
        if (!items.contains(device)) {
            items.add(device)
        }
    }

    fun getDevice(position: Int): BluetoothDevice {
        return items[position]
    }

    fun clear() {
        items.clear()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        return MyViewHolder(ItemBtDeviceBinding.inflate(LayoutInflater.from(parent.context)))
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val device = items[position]
        //An example of how to use the bindings
        holder.binding.contactName.text = device.name
        val button = holder.binding.messageButton
        button.setText(R.string.connect)
        button.setOnClickListener {
            holder.itemView.findNavController().navigate(R.id.action_HostFragment_to_Game, bundleOf("device" to device))
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }
}