package com.example.zerotrustauth.logic

import android.app.ActivityManager
import android.content.Context
import android.util.Log
import com.example.zerotrustauth.data.dataStore
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

/**
 * Manages secure wiping of application data
 */
object WipeManager {
    private val scope = CoroutineScope(Dispatchers.IO)

    /**
     * Performs a secure wipe of app data and restarts/kills the app
     */
    fun performSecureWipe(context: Context) {
        Log.w("WipeManager", "PERFORMING SECURE WIPE OF APPLICATION DATA!")
        
        scope.launch {
            try {
                // 1. Clear DataStore preferences
                context.dataStore.edit { it.clear() }
                
                // 2. Overwrite and delete internal files
                val internalDir = context.filesDir
                wipeDirectory(internalDir)
                
                // 3. Overwrite and delete cache
                wipeDirectory(context.cacheDir)

                // 4. Use system API to clear all app data (this effectively resets the app)
                val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
                activityManager.clearApplicationUserData()
                
            } catch (e: Exception) {
                Log.e("WipeManager", "Error during secure wipe", e)
            }
        }
    }

    private fun wipeDirectory(directory: File?) {
        directory?.listFiles()?.forEach { file ->
            if (file.isDirectory) {
                wipeDirectory(file)
            } else {
                // Simple overwrite with zeros before deletion for "security"
                try {
                    val length = file.length()
                    if (length > 0) {
                        file.writeBytes(ByteArray(length.toInt()) { 0 })
                    }
                } catch (e: Exception) {
                    // Ignore overwrite errors, proceed to delete
                }
                file.delete()
            }
        }
    }
}
