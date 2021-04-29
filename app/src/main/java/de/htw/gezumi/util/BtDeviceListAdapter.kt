package de.htw.gezumi.util

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import de.htw.gezumi.R

// Create the basic adapter extending from RecyclerView.Adapter
// Note that we specify the custom ViewHolder which gives us access to our views
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

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    inner class ViewHolder(listItemView: View) : RecyclerView.ViewHolder(listItemView) {
        // Your holder should contain and initialize a member variable
        // for any view that will be set as you render a row
        val nameTextView: TextView = itemView.findViewById(R.id.contact_name)
        val messageButton: Button = itemView.findViewById(R.id.message_button)
    }

    // ... constructor and member variables
    // Usually involves inflating a layout from XML and returning the holder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BtDeviceListAdapter.ViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        // Inflate the custom layout
        val contactView = inflater.inflate(R.layout.item_bt_device, parent, false)
        // Return a new holder instance
        return ViewHolder(contactView)
    }

    // Involves populating data into the item through holder
    override fun onBindViewHolder(viewHolder: BtDeviceListAdapter.ViewHolder, position: Int) {
        // Get the data model based on position
        val device: BluetoothDevice = mBtDevices[position]
        // Set item views based on your views and data model
        val textView = viewHolder.nameTextView
        textView.text = device.name
        val button = viewHolder.messageButton
        button.setText(R.string.connect)
        button.setOnClickListener {
            viewHolder.itemView.findNavController().navigate(R.id.action_HostFragment_to_Game, bundleOf("device" to device))
        }
    }

    // Returns the total count of items in the list
    override fun getItemCount(): Int {
        return mBtDevices.size
    }
}