package de.htw.gezumi

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.pm.PackageManager
import android.os.*
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.MutableLiveData
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
import de.htw.gezumi.viewmodel.GAME_ID_LENGTH
import de.htw.gezumi.viewmodel.GameViewModel


private const val TAG = "ClientFragment"

class ClientFragment : Fragment() {

    private val _gameViewModel: GameViewModel by activityViewModels()

    private lateinit var _binding: FragmentClientBinding
    private lateinit var _popupBinding: PopupJoinBinding
    private lateinit var _popupWindow: PopupWindow

    private val _availableHostDevices: ArrayList<Device> = ArrayList()
    private val _hostDeviceListAdapter: JoinGameListAdapter = JoinGameListAdapter(_availableHostDevices) {
        _gameViewModel.host = _availableHostDevices[it]

        val gattClientCallback = GattClientCallback(_gameViewModel)
        val gattClient = GattClient(requireContext())
        gattClient.connect(_availableHostDevices[it].bluetoothDevice, gattClientCallback)

        _gameViewModel.gameJoinUICallback = gameJoinUICallback
        _gameViewModel.gattClient = gattClient

        _popupBinding.joinText.text = getString(R.string.join_wait)
        _popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)
    }

    private val gameJoinUICallback = object : GameJoinUICallback {
        override fun gameJoined() {
            Handler(Looper.getMainLooper()).post{
                _popupWindow.dismiss()
                _popupBinding.joinText.text = getString(R.string.join_approved)
                _popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)
            }
        }

        override fun gameDeclined() {
            Handler(Looper.getMainLooper()).post{
                _popupWindow.dismiss()
                _popupBinding.joinText.text = getString(R.string.join_declined)
                _popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0)
            }
        }

        override fun gameStarted() {
            Handler(Looper.getMainLooper()).post{
                _popupWindow.dismiss()
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
                val deviceAddress = result.scanRecord!!.getManufacturerSpecificData(76)!!.sliceArray(GAME_ID_LENGTH until GAME_ID_LENGTH + 5)
                when (callbackType) {
                    ScanSettings.CALLBACK_TYPE_ALL_MATCHES -> {// first match does not have a name
                        if (!Utils.contains(_availableHostDevices, deviceAddress))
                            _availableHostDevices.add(Device(deviceAddress, result.txPower, result.device))
                        Utils.findDevice(_availableHostDevices, deviceAddress)!!.gameName.postValue(result.scanRecord!!.getManufacturerSpecificData(76)!!.sliceArray(6 until GAME_ID_LENGTH).toString(Charsets.UTF_8))
                        // read host rssi if already joined
                        //if (_gameViewModel.isJoined()) _gameViewModel.gameScanCallback.onScanResult(callbackType, result)
                    }
                    ScanSettings.CALLBACK_TYPE_MATCH_LOST -> {
                        _availableHostDevices.remove(Utils.findDevice(_availableHostDevices, deviceAddress)) // todo doesn't work with adapted scan settings
                        Log.d(TAG, "lost " + result.device.name)
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
    @SuppressLint("DefaultLocale")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding.lifecycleOwner = viewLifecycleOwner
        _binding.recyclerBtDevices.adapter = _hostDeviceListAdapter
        _binding.recyclerBtDevices.apply {
            setHasFixedSize(true)
            addItemDecoration(DividerItemDecoration(context, DividerItemDecoration.VERTICAL))
        }
        _binding.buttonScan.setOnClickListener {
            _gameViewModel.bluetoothController.startScan(_gameViewModel.hostScanCallback, _gameViewModel.gameId, true)// ParcelUuid(GameService.getGameId()), true) <- doesn't work, why???
        }

        checkPermission()

        _popupWindow = PopupWindow(_popupBinding.root, LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT, true)
    }

    override fun onPause() {
        super.onPause()
        // TODO maybe stop bluetooth scanning or smth
        updateBtDeviceListAdapter()
    }

    private fun updateBtDeviceListAdapter() {
        _hostDeviceListAdapter.notifyDataSetChanged()
    }

    private fun checkPermission() {
        if (!(context?.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && context?.checkSelfPermission(
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED)
        ) {
            ActivityCompat.requestPermissions(
                context as Activity, arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ), 1
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        if (!(requestCode == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED)) {
            checkPermission()
        }
    }

}