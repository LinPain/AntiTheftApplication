package com.example.zerotrustauth.logic

import android.content.Context
import android.util.Log
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.wearable.CapabilityClient
import com.google.android.gms.wearable.Wearable
import kotlinx.coroutines.tasks.await

/**
 * Model to handle synchronization of alarm states with wearable devices.
 * Fixes: getCapability fail with exception when Wearable API is unavailable.
 */
object WearableAlarmSyncModel {
    private const val TAG = "WearableAlarmSyncModel"
    private const val CAPABILITY_NAME = "anti_theft_alarm"

    suspend fun syncAlarmStatus(context: Context, isAlarmActive: Boolean) {
        if (!isWearableApiAvailable(context)) {
            Log.d(TAG, "Wearable API is not available on this device. Skipping sync.")
            return
        }

        try {
            Log.d(TAG, "Syncing alarm status: $isAlarmActive")
            val capabilityClient = Wearable.getCapabilityClient(context)
            // Use getCapability to find reachable nodes with the specific capability
            val capabilityInfo = capabilityClient
                .getCapability(CAPABILITY_NAME, CapabilityClient.FILTER_REACHABLE)
                .await()

            Log.d(TAG, "Found ${capabilityInfo.nodes.size} wearable nodes with capability $CAPABILITY_NAME")
            
            // Logic to send message to nodes would go here
            // For now, we are focusing on fixing the getCapability crash
            
        } catch (e: ApiException) {
            // This catches the API_UNAVAILABLE (statusCode=17) exception
            if (e.statusCode == 17) {
                Log.w(TAG, "Wearable.API is not available (statusCode=17). Connection failed.")
            } else {
                Log.e(TAG, "Wearable API error: ${e.message}", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error during wearable sync: ${e.message}", e)
        }
    }

    private fun isWearableApiAvailable(context: Context): Boolean {
        val availability = GoogleApiAvailability.getInstance()
        val resultCode = availability.isGooglePlayServicesAvailable(context)
        return resultCode == ConnectionResult.SUCCESS
    }
}
