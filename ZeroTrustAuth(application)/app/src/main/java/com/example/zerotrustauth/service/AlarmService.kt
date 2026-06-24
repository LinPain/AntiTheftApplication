package com.example.zerotrustauth.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.zerotrustauth.MainActivity
import com.example.zerotrustauth.network.LocationApiService
import com.example.zerotrustauth.data.SecurityPrefs
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

class AlarmService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private val apiService = LocationApiService.create()
    private lateinit var prefs: SecurityPrefs
    private var isAlarmPlaying = false

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
        return START_STICKY
    }

    private fun startPolling() {
        scope.launch {
            while (isActive) {
                try {
                    val status = apiService.checkFullStatus()
                    
                    // Handle Alarm
                    if (status.alarm.active && !isAlarmPlaying) {
                        playAlarm()
                    } else if (!status.alarm.active && isAlarmPlaying) {
                        stopAlarm()
                    }

                    // Handle Lockdown
                    val wasActive = prefs.isRemoteLockdownActive.first()
                    if (status.lockdown.active && !wasActive) {
                        forceOpenApp()
                        showLockdownNotification()
                    }
                    prefs.setRemoteLockdown(status.lockdown.active)

                } catch (e: Exception) {
                    Log.e("AlarmService", "Polling error", e)
                }
                delay(POLLING_INTERVAL)
            }
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
        Log.w("AlarmService", "Forcing app to open due to remote lockdown")
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
        }
        try {
            startActivity(launchIntent)
        } catch (e: Exception) {
            Log.e("AlarmService", "Could not start activity from background", e)
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
        scope.cancel()
    }
}
