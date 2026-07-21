package com.example.zerotrustauth.logic

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import android.provider.Settings
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks

class LocationHelper(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    fun getDeviceId(): String {
        val id = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
        return "android_$id"
    }

    fun getDeviceName(): String {
        val manufacturer = android.os.Build.MANUFACTURER
        val model = android.os.Build.MODEL
        return if (model.startsWith(manufacturer, ignoreCase = true)) {
            model.replaceFirstChar { it.uppercase() }
        } else {
            "${manufacturer.replaceFirstChar { it.uppercase() }} $model"
        }
    }

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(): Task<Location?> {
        // Try to get last location first
        return fusedLocationClient.lastLocation.continueWithTask { task ->
            val location = if (task.isSuccessful) task.result else null
            if (location != null && (System.currentTimeMillis() - location.time) < 60000) {
                // If last location is recent (last 1 min), return it
                Log.d("LocationHelper", "Using recent lastLocation")
                Tasks.forResult(location)
            } else {
                // Otherwise request a fresh one-time location
                Log.d("LocationHelper", "Requesting fresh current location")
                fusedLocationClient.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                )
            }
        }
    }
}
