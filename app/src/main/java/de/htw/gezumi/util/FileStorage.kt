package de.htw.gezumi.util

import android.content.Context
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

private const val TAG = "FileStorage"

class FileStorage {
    companion object {
        /**
         * Writes a file with the given [fileName] and [body] into external storage.
         */
        fun writeFile(context: Context, fileName: String, body: String) {
            val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: return
            val file = File(dir, fileName)
            var os: FileOutputStream?
            try {
                dir.mkdirs()
                os = FileOutputStream(file)
                os.write(body.toByteArray())
                os.close()
                Log.d(TAG, "wrote file $fileName into external storage. Path: $dir.absolutePath")
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}