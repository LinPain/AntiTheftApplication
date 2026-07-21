package com.example.zerotrustauth.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.PixelFormat
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.example.zerotrustauth.MainActivity
import com.example.zerotrustauth.network.LocationApiService
import com.example.zerotrustauth.data.SecurityPrefs
import com.example.zerotrustauth.logic.WearableAlarmSyncModel
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.net.URISyntaxException

/**
 * Command Center Service
 */
class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var prefs: SecurityPrefs
    private var isAlarmPlaying = false
    private var socket: Socket? = null
    private var overlayView: View? = null
    private val windowManager by lazy { getSystemService(Context.WINDOW_SERVICE) as WindowManager }
    private var lastTrackRequestTime = 0L

    companion object {
        private const val NOTIFICATION_ID = 5678
        private const val CHANNEL_ID = "alarm_channel"
        private const val LOCKDOWN_NOTIFICATION_ID = 9999
        private const val POLLING_INTERVAL = 5000L
    }

    override fun onCreate() {
        super.onCreate()
        prefs = SecurityPrefs(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        val notification = createNotification()
        
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE or ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                @Suppress("DEPRECATION")
                startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_NONE)
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
        } catch (e: Exception) {
            startForeground(NOTIFICATION_ID, notification)
        }
        
        startPolling()
        startEnforcementLoop()
        initSocketConnection()
        return START_STICKY
    }

    private fun initSocketConnection() {
        scope.launch {
            val username = prefs.username.first()
            val token = prefs.authToken.first()
            if (username == null || username == "guest" || token == null) return@launch

            try {
                val opts = IO.Options().apply {
                    auth = mapOf("token" to token)
                    extraHeaders = mapOf("ngrok-skip-browser-warning" to listOf("true"))
                }
                
                socket = IO.socket("https://pardon-resolute-outscore.ngrok-free.dev/", opts)
                socket?.on(Socket.EVENT_CONNECT) { socket?.emit("join", username.lowercase()) }

                socket?.on("trackRequested") {
                    Log.i("AlarmService", "Real-time Track Received")
                    LocationService.triggerImmediateUpload(this@AlarmService)
                }

                socket?.on("statusUpdate") { _ ->
                    Log.d("AlarmService", "Syncing status via socket")
                    scope.launch {
                        syncFullStatus(username, token)
                    }
                }

                socket?.connect()
            } catch (e: URISyntaxException) {
                Log.e("AlarmService", "Socket Error: ${e.message}")
            }
        }
    }

    private fun startPolling() {
        scope.launch {
            while (isActive) {
                val user = prefs.username.first()
                val token = prefs.authToken.first()
                if (user != null && user != "guest" && token != null) {
                    syncFullStatus(user, token)
                }
                delay(POLLING_INTERVAL)
            }
        }
    }

    private suspend fun syncFullStatus(username: String, token: String) {
        try {
            val status = LocationApiService.create(token).checkFullStatus(username)
            
            if (status.alarm.active && !isAlarmPlaying) playAlarm()
            else if (!status.alarm.active && isAlarmPlaying) stopAlarm()

            // Sync alarm status to wearable
            WearableAlarmSyncModel.syncAlarmStatus(this, status.alarm.active)

            val wasLocked = prefs.isRemoteLockdownActive.first()
            if (status.lockdown.active && !wasLocked) showLockdownNotification()
            prefs.setRemoteLockdown(status.lockdown.active)

            status.lostMode?.let { lost ->
                prefs.setLostMode(lost.active, lost.message, lost.phoneNumber)
            }

            status.trackRequest?.let { req ->
                if (req.active && req.timestamp > lastTrackRequestTime) {
                    lastTrackRequestTime = req.timestamp
                    LocationService.triggerImmediateUpload(this@AlarmService)
                }
            }
        } catch (e: Exception) {
            Log.e("AlarmService", "Status sync failed")
        }
    }

    private fun startEnforcementLoop() {
        scope.launch {
            while (isActive) {
                val isRemoteLocked = prefs.isRemoteLockdownActive.first()
                val isLost = prefs.isLostModeActive.first()
                if (isRemoteLocked || isLost) {
                    forceOpenApp()
                    if (Settings.canDrawOverlays(this@AlarmService)) {
                        withContext(Dispatchers.Main) { showOverlay() }
                    }
                } else {
                    withContext(Dispatchers.Main) { removeOverlay() }
                }
                delay(500L)
            }
        }
    }

    private fun showOverlay() {
        if (overlayView != null) return
        try {
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply { gravity = Gravity.CENTER }
            overlayView = View(this).apply { setBackgroundColor(0x01000000) }
            windowManager.addView(overlayView, params)
        } catch (e: Exception) { }
    }

    private fun removeOverlay() {
        if (overlayView == null) return
        try {
            windowManager.removeView(overlayView)
            overlayView = null
        } catch (e: Exception) { }
    }

    private fun playAlarm() {
        if (isAlarmPlaying) return
        isAlarmPlaying = true
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM) ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, uri)
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
                isLooping = true
                prepare()
                start()
            }
            val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            am.setStreamVolume(AudioManager.STREAM_ALARM, am.getStreamMaxVolume(AudioManager.STREAM_ALARM), 0)
            
            scope.launch { WearableAlarmSyncModel.syncAlarmStatus(this@AlarmService, true) }
        } catch (e: Exception) { isAlarmPlaying = false }
    }

    private fun stopAlarm() {
        isAlarmPlaying = false
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
        scope.launch { WearableAlarmSyncModel.syncAlarmStatus(this@AlarmService, false) }
    }

    private fun forceOpenApp() {
        val intent = Intent(this, MainActivity::class.java).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT) }
        try {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE).send()
        } catch (e: Exception) {
            try { startActivity(intent) } catch (ex: Exception) { }
        }
    }

    private fun showLockdownNotification() {
        val intent = Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }
        val pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("THIẾT BỊ ĐÃ BỊ KHOÁ").setContentText("Vui lòng mở ứng dụng để xác thực danh tính.")
            .setSmallIcon(android.R.drawable.ic_lock_lock).setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM).setFullScreenIntent(pi, true)
            .setOngoing(true).build()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(LOCKDOWN_NOTIFICATION_ID, n)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Bảo mật Zero Trust đang hoạt động").setContentText("Đang giám sát các lệnh khẩn cấp từ xa")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock).setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true).build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val c = NotificationChannel(CHANNEL_ID, "Lệnh điều khiển từ xa", NotificationManager.IMPORTANCE_HIGH)
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(c)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
        removeOverlay()
        socket?.disconnect()
        scope.cancel()
    }
}
