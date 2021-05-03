package de.htw.gezumi

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.DividerItemDecoration
import de.htw.gezumi.adapter.PlayerDeviceListAdapter
import de.htw.gezumi.databinding.FragmentHostBinding

class HostFragment : Fragment() {

    private lateinit var _binding: FragmentHostBinding

    private val _bluetoothController: BluetoothController = BluetoothController()
    private lateinit var _gattServer: GattServer
    private val _playerListAdapter: PlayerDeviceListAdapter = PlayerDeviceListAdapter(listOf()) //TODO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
        // start gatt server
        _gattServer = GattServer(requireContext(), _bluetoothController)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_host, container, false)
        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding.lifecycleOwner = viewLifecycleOwner
        _binding.recyclerPlayers.adapter = _playerListAdapter
        _binding.recyclerPlayers.apply {
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
    }

    override fun onStart() {
        super.onStart()
        _gattServer.registerTimeServiceReceiver()
    }

    override fun onStop() {
        super.onStop()
        _gattServer.unregisterTimeServiceReceiver()
    }

    override fun onDestroy() {
        super.onDestroy()
        _gattServer.stop()
    }

    override fun onPause() {
        super.onPause()
        //deviceListAdapter.clear() should we really clear all bluetooth devices here?
        // TODO maybe stop bluetooth scanning or smth
        updatePlayerListAdapter();
    }

    fun updatePlayerListAdapter() {
        _playerListAdapter.notifyDataSetChanged()
    }
}