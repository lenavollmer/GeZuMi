package de.htw.gezumi

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
import de.htw.gezumi.controller.HOST_SCAN_KEY
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

    private var _gameStarted = false

    private val _availableHostDevices: ArrayList<Device> = ArrayList()
    private val _hostDeviceListAdapter: JoinGameListAdapter = JoinGameListAdapter(_availableHostDevices) {

        _gameViewModel.host = _availableHostDevices[it]

        val gattClientCallback = GattClientCallback()
        _gattClient.connect(_availableHostDevices[it].bluetoothDevice!!, gattClientCallback)

        _gameViewModel.gameJoinUICallback = gameJoinUICallback
        _gameViewModel.gattClient = _gattClient

        _popupBinding.joinText.text = getString(R.string.join_wait)
        _popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)

        _availableHostDevices.clear()
        updateBtDeviceListAdapter()
    }

    private val gameJoinUICallback = object : GameJoinUICallback {
        override fun gameJoined() {
            Handler(Looper.getMainLooper()).post {
                _popupWindow.dismiss()
                _popupBinding.joinText.text = getString(R.string.join_approved)
                _popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)

                _availableHostDevices.clear()
                updateBtDeviceListAdapter()
            }
        }

        override fun gameDeclined() {
            Handler(Looper.getMainLooper()).post {
                _popupWindow.dismiss()
                _popupBinding.joinText.text = getString(R.string.join_declined)
                _popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)

                _gameViewModel.bluetoothController.stopScan(HOST_SCAN_KEY)
                _gattClient.disconnect() // functionality should not be in UI callback

                _availableHostDevices.clear()
                updateBtDeviceListAdapter()
            }
        }

        override fun gameStarted() {
            Handler(Looper.getMainLooper()).post {
                _gameStarted = true
                _popupWindow.dismiss()
                Log.d(TAG, "game started")
                findNavController().navigate(R.id.action_ClientFragment_to_Game)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _gameViewModel.gameId = GameService.GAME_ID_PREFIX

        _gameViewModel.hostScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                super.onScanResult(callbackType, result)
                val deviceId = GameService.extractDeviceId(result)
                when (callbackType) {
                    ScanSettings.CALLBACK_TYPE_ALL_MATCHES -> {
                        if (!Utils.contains(_availableHostDevices, deviceId)) {
                            _availableHostDevices.add(Device(deviceId, result.txPower, result.device))
                            return
                        }

                        val device = Utils.findDevice(_availableHostDevices, deviceId)!!
                        // check for new game name
                        val newGameName = GameService.extractGameName(result)
                        if (!device.gameName.equals(newGameName))
                            device.gameName.postValue(newGameName)
                        // refresh bt device: if game name changed, host uses a new bt device
                        if (device.bluetoothDevice != result.device)
                            device.bluetoothDevice = result.device
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

        _hostDeviceListAdapter.lifecycleOwner = viewLifecycleOwner

        return _binding.root
    }

    @kotlin.ExperimentalUnsignedTypes
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding.lifecycleOwner = viewLifecycleOwner
        _binding.recyclerBtDevices.adapter = _hostDeviceListAdapter
        _binding.recyclerBtDevices.apply {
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
        _binding.buttonScan.setOnClickListener {
            _availableHostDevices.clear()
            updateBtDeviceListAdapter()
            _gameViewModel.bluetoothController.startHostScan(_gameViewModel.hostScanCallback)// ParcelUuid(GameService.getGameId()), true) <- doesn't work, why???
        }

        _popupWindow = PopupWindow(
            _popupBinding.root,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        )
        _popupWindow.isOutsideTouchable = false
        _popupWindow.isFocusable = false


        _gattClient = GattClient(requireContext())
    }

    override fun onPause() {
        super.onPause()
        _popupWindow.dismiss()
        if(!_gameStarted){
            _gattClient.disconnect()
        }
        _availableHostDevices.clear()
        updateBtDeviceListAdapter()
    }

    override fun onDestroy() {
        super.onDestroy()
        _gameViewModel.bluetoothController.stopScan(HOST_SCAN_KEY)
    }

    private fun updateBtDeviceListAdapter() {
        _hostDeviceListAdapter.notifyDataSetChanged()
    }

}