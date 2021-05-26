package de.htw.gezumi

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.*
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import de.htw.gezumi.adapter.JoinGameListAdapter
import de.htw.gezumi.callbacks.GameJoinUICallback
import de.htw.gezumi.databinding.FragmentClientBinding
import de.htw.gezumi.databinding.PopupJoinBinding
import de.htw.gezumi.gatt.GameService
import de.htw.gezumi.gatt.GattClient
import de.htw.gezumi.gatt.GattClientCallback
import de.htw.gezumi.model.Device
import de.htw.gezumi.viewmodel.GameViewModel


private const val TAG = "ClientFragment"

class ClientFragment : Fragment() {

    private val _gameViewModel: GameViewModel by activityViewModels()

    private lateinit var _binding: FragmentClientBinding
    private lateinit var _popupBinding: PopupJoinBinding
    private lateinit var _popupWindow: PopupWindow
    private lateinit var _gattClient: GattClient

    private val _availableHostDevices: ArrayList<BluetoothDevice> = ArrayList()
    private val _hostDeviceListAdapter: JoinGameListAdapter = JoinGameListAdapter(_availableHostDevices) {
        val hostDevice = Device(_availableHostDevices[it].address, -70, _availableHostDevices[it])
        hostDevice.setName(_availableHostDevices[it].address)
        _gameViewModel.host = hostDevice

        val gattClientCallback = GattClientCallback(_gameViewModel)
        _gattClient.connect(_availableHostDevices[it], gattClientCallback)

        _gameViewModel.gameJoinUICallback = gameJoinUICallback
        _gameViewModel.gattClient = _gattClient

        _popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)
    }

    private val gameJoinUICallback = object : GameJoinUICallback {
        override fun gameJoined() {
            Handler(Looper.getMainLooper()).post {
                _popupWindow.dismiss()
                _popupBinding.joinText.text = getString(R.string.join_approved)
                _popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)
            }
        }

        override fun gameDeclined() {
            Handler(Looper.getMainLooper()).post {
                _popupWindow.dismiss()
                // Todo Bug: This shows up when trying to reconnect
                _popupBinding.joinText.text = getString(R.string.join_declined)
                _popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)
                //findNavController().navigate(R.id.action_Client_self)

                _gameViewModel.bluetoothController.stopScan(_gameViewModel.hostScanCallback)
                _gattClient.disconnect()
                _availableHostDevices.clear()
                updateBtDeviceListAdapter()
            }
        }

        override fun gameStarted() {
            Handler(Looper.getMainLooper()).post {
                _popupWindow.dismiss()
                findNavController().navigate(R.id.action_ClientFragment_to_Game)
            }
        }

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _gameViewModel.hostScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                Log.d(TAG, "host scan callback")
                when (callbackType) {
                    // first match does not have a name -> https://stackoverflow.com/questions/43606975/scan-ble-android-getname-or-device-not-complete-or-null
                    ScanSettings.CALLBACK_TYPE_FIRST_MATCH -> {
                        if (!_availableHostDevices.contains(result.device)) _availableHostDevices.add(result.device)
                    }
                    ScanSettings.CALLBACK_TYPE_MATCH_LOST -> {
                        _availableHostDevices.remove(result.device)
                    }
                }
                updateBtDeviceListAdapter()
            }

            override fun onScanFailed(errorCode: Int) {
                super.onScanFailed(errorCode)
                Log.d(TAG, "scan failed: $errorCode")
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_client, container, false)
        _popupBinding = DataBindingUtil.inflate(inflater, R.layout.popup_join, null, false)

        return _binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding.lifecycleOwner = viewLifecycleOwner
        _binding.recyclerBtDevices.adapter = _hostDeviceListAdapter
        _binding.recyclerBtDevices.apply {
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
        _binding.buttonScan.setOnClickListener {
            _gameViewModel.bluetoothController.startScan(
                _gameViewModel.hostScanCallback,
                ParcelUuid(GameService.HOST_UUID),
                true
            )// ParcelUuid(GameService.getGameId()), true) <- doesn't work, why???
        }

        _popupWindow = PopupWindow(
            _popupBinding.root,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )

        _gattClient = GattClient(requireContext())
    }

    override fun onPause() {
        super.onPause()
        _popupWindow.dismiss()
        _gameViewModel.bluetoothController.stopScan(_gameViewModel.hostScanCallback)
        _gattClient.disconnect()
        _availableHostDevices.clear()
        updateBtDeviceListAdapter()
    }

    private fun updateBtDeviceListAdapter() {
        _hostDeviceListAdapter.notifyDataSetChanged()
    }

}