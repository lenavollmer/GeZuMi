package de.htw.gezumi.viewmodel

import android.content.Context
import android.os.Environment
import android.util.Log
import androidx.lifecycle.ViewModel
import de.htw.gezumi.model.Device
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


private const val TAG = "DevicesViewModel"

class DevicesViewModel : ViewModel() {

    private val _devices = mutableListOf<Device>()
    val devices: List<Device> get() = _devices

    lateinit var host: Device

    fun addDevice(device: Device) {
        _devices.add(device)
    }

    fun writeRSSILog(context: Context?) {
        if (context == null) return
        writeFile(
            context,
            "${java.util.Calendar.getInstance().time.toString()}_distance_log.txt",
            devices[0].rssiHistory.toString()
        )
    }

    fun writeFile(context: Context, sFileName: String, sBody: String) {
        val dir: File = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return
//        val dir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val file = File(dir, sFileName)
        var os: FileOutputStream? = null
        dir.mkdirs()
        file.mkdirs()
        try {
            os = FileOutputStream(file)
            os.write(sBody.toByteArray())
            os.close()
            Log.d(TAG, "wrote file $sFileName into external storage. Path: $dir.absolutePath")
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }


}