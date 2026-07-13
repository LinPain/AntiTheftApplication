package com.example.zerotrustauth.logic

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks

class LocationHelper(private val context: Context) {
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    fun getCurrentLocation(): Task<Location?> {
        // Try to get last location first
        return fusedLocationClient.lastLocation.continueWithTask { task ->
            val location = task.result
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
