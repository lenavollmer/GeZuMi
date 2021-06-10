package de.htw.gezumi

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import de.htw.gezumi.databinding.FragmentPermissionBinding


/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class PermissionFragment : Fragment() {

    private lateinit var _binding: FragmentPermissionBinding


    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions.all { it.value == true }) {
                findNavController().navigate(R.id.action_MainFragment_to_MainMenuFragment)
            } else {
                _binding.mainText.text = getString(R.string.permission_denied)
                _binding.requestPermission.setOnClickListener {
                    requireContext().openAppSystemSettings()
                }
            }
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        _binding = DataBindingUtil.inflate(inflater, R.layout.fragment_permission, container, false)
        return _binding.root
    }

    private fun Context.openAppSystemSettings() {
        startActivity(Intent().apply {
            action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
            data = Uri.fromParts("package", packageName, null)
        })
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        _binding.requestPermission.setOnClickListener {
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }

        if (BluetoothAdapter.getDefaultAdapter() == null) {
            Toast.makeText(
                context,
                getString(R.string.bluetooth_not_supported),
                Toast.LENGTH_LONG
            ).show()
        } else {
            requestMultiplePermissions.launch(
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
                )
            )
        }
    }

    override fun onResume() {
        super.onResume()
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            findNavController().navigate(R.id.action_MainFragment_to_MainMenuFragment)
        }
    }
}