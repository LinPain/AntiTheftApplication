package com.example.zerotrustauth.service

import android.app.*
import android.content.Context
import android.content.Intent
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
import androidx.core.app.NotificationCompat
import com.example.zerotrustauth.MainActivity
import com.example.zerotrustauth.network.LocationApiService
import com.example.zerotrustauth.data.SecurityPrefs
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.net.URISyntaxException

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var prefs: SecurityPrefs
    private var isAlarmPlaying = false
    private var socket: Socket? = null
    private var overlayView: View? = null
    private val windowManager by lazy { getSystemService(Context.WINDOW_SERVICE) as WindowManager }

    companion object {
        private const val NOTIFICATION_ID = 5678
        private const val CHANNEL_ID = "alarm_channel"
        private const val LOCKDOWN_NOTIFICATION_ID = 9999
        private const val POLLING_INTERVAL = 5000L // 5 seconds
    }

    override fun onCreate() {
        super.onCreate()
        prefs = SecurityPrefs(applicationContext)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        startPolling()
        startEnforcementLoop()
        initSocketConnection()
        return START_STICKY
    }

    private fun initSocketConnection() {
        scope.launch {
            val username = prefs.username.first()
            val token = prefs.authToken.first()

            if (username != null && username != "guest" && token != null) {
                try {
                    val opts = IO.Options()
                    opts.auth = mapOf("token" to token)
                    opts.extraHeaders = mapOf("ngrok-skip-browser-warning" to listOf("true"))
                    
                    socket = IO.socket("https://pardon-resolute-outscore.ngrok-free.dev/", opts)
                    
                    socket?.on(Socket.EVENT_CONNECT) {
                        Log.i("AlarmService", "Socket connected! Joining room: $username")
                        socket?.emit("join", username)
                    }

                    socket?.on("statusUpdate") {
                        Log.d("AlarmService", "Status update event received via socket")
                        // We could trigger an immediate poll here if we wanted
                    }

                    socket?.connect()
                } catch (e: URISyntaxException) {
                    Log.e("AlarmService", "Socket URI error", e)
                }
            }
        }
    }

    private fun startPolling() {
        scope.launch {
            while (isActive) {
                try {
                    val username = prefs.username.first()
                    val token = prefs.authToken.first()

                    if (username != null && username != "guest") {
                        val apiService = LocationApiService.create(token)
                        Log.d("AlarmService", "Polling status for user: $username")
                        val status = apiService.checkFullStatus(username)
                        Log.d("AlarmService", "Status received: alarm=${status.alarm.active}, lockdown=${status.lockdown.active}")

                        // Handle Alarm
                        if (status.alarm.active && !isAlarmPlaying) {
                            playAlarm()
                        } else if (!status.alarm.active && isAlarmPlaying) {
                            stopAlarm()
                        }

                        // Handle Lockdown Status Update
                        val wasActive = prefs.isRemoteLockdownActive.first()
                        if (status.lockdown.active && !wasActive) {
                            Log.w("AlarmService", "Lockdown state CHANGED to ACTIVE. Triggering immediate notification.")
                            showLockdownNotification()
                        }
                        prefs.setRemoteLockdown(status.lockdown.active)
                    } else {
                        Log.d("AlarmService", "Skipping poll: No user logged in")
                    }
                } catch (e: Exception) {
                    Log.e("AlarmService", "Polling error: ${e.message}", e)
                }
                delay(POLLING_INTERVAL)
            }
        }
    }

    private fun startEnforcementLoop() {
        scope.launch {
            while (isActive) {
                try {
                    val isLocked = prefs.isRemoteLockdownActive.first()
                    if (isLocked) {
                        Log.d("AlarmService", "[ENFORCEMENT] Lockdown is active. Forcing app to foreground and enabling overlay.")
                        forceOpenApp()
                        withContext(Dispatchers.Main) {
                            showOverlay()
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            removeOverlay()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("AlarmService", "Enforcement error: ${e.message}", e)
                }
                delay(500L) // Increased frequency to 500ms for high responsiveness
            }
        }
    }

    private fun showOverlay() {
        if (overlayView != null) return

        try {
            val params = WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                else
                    WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.CENTER
            }

            overlayView = View(this).apply {
                // Set background to semi-transparent to indicate lockdown but still see the app
                setBackgroundColor(0x01000000) // Nearly transparent but consumes touches
            }

            windowManager.addView(overlayView, params)
            Log.i("AlarmService", "Lockdown overlay ADDED")
        } catch (e: Exception) {
            Log.e("AlarmService", "Failed to add overlay", e)
        }
    }

    private fun removeOverlay() {
        if (overlayView == null) return
        try {
            windowManager.removeView(overlayView)
            overlayView = null
            Log.i("AlarmService", "Lockdown overlay REMOVED")
        } catch (e: Exception) {
            Log.e("AlarmService", "Failed to remove overlay", e)
        }
    }

    private fun playAlarm() {
        Log.w("AlarmService", "ACTIVATING REMOTE ALARM!")
        isAlarmPlaying = true
        
        try {
            val alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(applicationContext, alarmUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }

            // Set volume to max
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.setStreamVolume(
                AudioManager.STREAM_ALARM,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                0
            )
        } catch (e: Exception) {
            Log.e("AlarmService", "Error playing alarm", e)
        }
    }

    private fun stopAlarm() {
        Log.i("AlarmService", "Stopping remote alarm")
        isAlarmPlaying = false
        mediaPlayer?.stop()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    private fun forceOpenApp() {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }
        
        try {
            // Primary method: Direct startActivity
            startActivity(launchIntent)
        } catch (e: Exception) {
            Log.e("AlarmService", "Direct startActivity failed, trying PendingIntent", e)
            try {
                // Secondary method: PendingIntent (more likely to succeed if background restricted)
                val pendingIntent = PendingIntent.getActivity(
                    this, 0, launchIntent, 
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                pendingIntent.send()
            } catch (pendingEx: Exception) {
                Log.e("AlarmService", "All foreground enforcement methods failed", pendingEx)
            }
        }
    }

    private fun showLockdownNotification() {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("THIẾT BỊ ĐÃ BỊ KHOÁ")
            .setContentText("Vui lòng mở ứng dụng để xác thực danh tính.")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true) // FALLBACK FOR BACKGROUND START
            .setAutoCancel(false)
            .setOngoing(true)
            .build()

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(LOCKDOWN_NOTIFICATION_ID, notification)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Bảo mật Zero Trust đang hoạt động")
            .setContentText("Đang giám sát các lệnh khẩn cấp từ xa")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Lệnh điều khiển từ xa",
                NotificationManager.IMPORTANCE_HIGH // HIGH FOR FULL SCREEN INTENT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        stopAlarm()
        removeOverlay()
        socket?.disconnect()
        socket?.off()
        scope.cancel()
    }
}
