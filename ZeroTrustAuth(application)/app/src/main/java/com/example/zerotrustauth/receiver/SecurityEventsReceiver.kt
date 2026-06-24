package com.example.zerotrustauth.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import android.util.Log
import com.example.zerotrustauth.data.SecurityPrefs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SecurityEventsReceiver : BroadcastReceiver() {
    private val scope = CoroutineScope(Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        val prefs = SecurityPrefs(context)
        
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED, "android.intent.action.SIM_STATE_CHANGED" -> {
                checkSimState(context, prefs)
            }
            "android.intent.action.SCREEN_OFF" -> {
                // Potential placeholder for tracking lock state
            }
            // Note: Detecting failed unlock attempts specifically usually requires DeviceAdmin
            // or AccessibilityService. For this showcase, we'll simulate or use a 
            // simplified approach if possible, but ACTION_USER_PRESENT (success)
            // can be used to reset the counter.
            Intent.ACTION_USER_PRESENT -> {
                scope.launch {
                    prefs.resetFailedUnlock()
                    prefs.updateInternetTimestamp(System.currentTimeMillis())
                }
            }
        }
    }

    private fun checkSimState(context: Context, prefs: SecurityPrefs) {
        val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        
        // Note: On Android 10+, getSimSerialNumber is restricted.
        // We use simOperatorName as a simpler proxy for this showcase.
        val currentSimId = try {
            tm.simOperatorName ?: "unknown"
        } catch (e: SecurityException) {
            "restricted"
        }
        
        scope.launch {
            val lastSimId = prefs.lastSimId.first()
            if (lastSimId != null && lastSimId != currentSimId && currentSimId != "unknown") {
                Log.w("SecurityReceiver", "SIM CARD CHANGED! Potential risk detected.")
            }
            prefs.saveSimId(currentSimId)
        }
    }
}
