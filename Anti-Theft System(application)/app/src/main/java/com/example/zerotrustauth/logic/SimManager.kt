package com.example.zerotrustauth.logic

import android.content.Context
import android.util.Log
import com.example.zerotrustauth.data.SecurityPrefs
import com.example.zerotrustauth.network.LocationApiService
import com.example.zerotrustauth.network.SimAlertRequest
import com.example.zerotrustauth.service.LocationService
import kotlinx.coroutines.flow.first
import android.app.NotificationManager
import android.app.NotificationChannel
import android.os.Build
import androidx.core.app.NotificationCompat

object SimManager {

    suspend fun notifySimChange(context: Context, newOperator: String) {
        val prefs = SecurityPrefs(context)
        val username = prefs.username.first() ?: return
        
        Log.w("SimManager", "SIM CHANGE DETECTED! Operator: $newOperator")

        // 1. Trigger Immediate Location Upload
        LocationService.triggerImmediateUpload(context)

        // 2. Notify Backend (Email)
        try {
            val apiService = LocationApiService.create()
            apiService.notifySimAlert(SimAlertRequest(username, newOperator))
        } catch (e: Exception) {
            Log.e("SimManager", "Failed to send SIM alert: ${e.message}")
        }

        // 3. Show Local Alert Notification
        showLocalAlert(context, newOperator)
    }

    private fun showLocalAlert(context: Context, operator: String) {
        val channelId = "sim_alert_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "SIM Security Alerts", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle("⚠️ CẢNH BÁO THAY ĐỔI SIM")
            .setContentText("Phát hiện thẻ SIM mới: $operator. Vĩ độ/Kinh độ đã được tải lên.")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(8888, notification)
    }
}
