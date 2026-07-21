package com.example.zerotrustauth.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.zerotrustauth.logic.LocationHelper
import com.example.zerotrustauth.network.LocationApiService
import com.example.zerotrustauth.network.LocationRequest
import com.example.zerotrustauth.data.SecurityPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class UnlockReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_USER_PRESENT) {
            Log.d("UnlockReceiver", "Screen unlocked! Fetching and saving location...")
            saveLocation(context)
        }
    }

    private fun saveLocation(context: Context) {
        val locationHelper = LocationHelper(context)
        val prefs = SecurityPrefs(context)

        locationHelper.getCurrentLocation().addOnSuccessListener { location ->
            location?.let {
                scope.launch {
                    try {
                        val token = prefs.authToken.first()
                        val username = prefs.username.first() ?: "guest"
                        val apiService = LocationApiService.create(token)

                        apiService.sendLocation(
                            username = username,
                            location = LocationRequest(
                                deviceId = locationHelper.getDeviceId(),
                                latitude = it.latitude,
                                longitude = it.longitude
                            )
                        )
                        Log.d("UnlockReceiver", "Location saved successfully on unlock")
                    } catch (e: Exception) {
                        Log.e("UnlockReceiver", "Failed to save location on unlock", e)
                    }
                }
            }
        }
    }
}
