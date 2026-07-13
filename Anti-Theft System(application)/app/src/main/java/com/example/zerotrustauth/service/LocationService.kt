package com.example.zerotrustauth.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.zerotrustauth.network.LocationApiService
import com.example.zerotrustauth.network.LocationRequest as NetworkLocationRequest
import com.google.android.gms.location.*
import com.example.zerotrustauth.data.SecurityPrefs
import com.example.zerotrustauth.logic.LocationHelper
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.lifecycle.LifecycleService
import com.example.zerotrustauth.logic.CameraHelper
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.net.URISyntaxException

class LocationService : LifecycleService() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var prefs: SecurityPrefs
    private lateinit var locationHelper: LocationHelper
    private var lastTrackRequestTimestamp = 0L
    private var socket: Socket? = null

    companion object {
        private const val NOTIFICATION_ID = 1234
        private const val CHANNEL_ID = "location_channel"
        private const val POLLING_INTERVAL = 5000L 
        var isRunning = false
        
        private var instance: LocationService? = null
        
        fun triggerIntruderCapture() {
            instance?.let { 
                Log.i("LocationService", "Triggering intruder capture via singleton instance")
                CameraHelper.captureAndUpload(it, it) 
            }
        }

        fun triggerImmediateUpload() {
            instance?.let {
                Log.i("LocationService", "Triggering immediate location upload")
                it.scope.launch {
                    val token = it.prefs.authToken.first()
                    val username = it.prefs.username.first()
                    if (username != null && username != "guest" && token != null) {
                        val apiService = LocationApiService.create(token)
                        it.sendImmediateLocation(apiService, username)
                    }
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        prefs = SecurityPrefs(applicationContext)
        locationHelper = LocationHelper(applicationContext)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        startLocationUpdates()
        isRunning = true
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Real-time Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).apply {
            setMinUpdateIntervalMillis(2000L)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
                    Log.d("LocationService", "New location: \${location.latitude}, \${location.longitude}")
                    sendLocationToBackend(location.latitude, location.longitude)
                    updateNotification(location.latitude, location.longitude)
                    checkGeofence(location.latitude, location.longitude)
                }
            }
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Đang theo dõi vị trí")
            .setContentText("Hệ thống Zero Trust đang chạy ngầm")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    private fun checkGeofence(lat: Double, lon: Double) {
        scope.launch {
            val safeLat = prefs.safeZoneLat.first()
            val safeLon = prefs.safeZoneLon.first()
            val radius = prefs.safeZoneRadius.first()

            if (safeLat != null && safeLon != null) {
                val results = FloatArray(1)
                android.location.Location.distanceBetween(lat, lon, safeLat, safeLon, results)
                val distance = results[0]

                if (distance > radius) {
                    Log.w("LocationService", "OUTSIDE SAFE ZONE! Distance: \$distance m")
                    prefs.incrementFailedUnlock()
                }
            }
        }
    }

    private fun updateNotification(lat: Double, lon: Double) {
        val notificationManager = getSystemService(NotificationManager::class.java)
        val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
        
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("📍 Vị trí đã cập nhật")
            .setContentText("Gửi lúc \$time: \$lat, \$lon")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
        
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun sendLocationToBackend(lat: Double, lon: Double) {
        scope.launch {
            try {
                val token = prefs.authToken.first()
                val username = prefs.username.first() ?: "guest"
                
                if (username == "guest" || token.isNullOrBlank()) {
                    return@launch
                }
                
                val apiService = LocationApiService.create(token)

                apiService.sendLocation(
                    username = username,
                    location = NetworkLocationRequest(
                        deviceId = "android_device_1",
                        latitude = lat,
                        longitude = lon
                    )
                )
            } catch (e: Exception) {
                Log.e("LocationService", "Error sending location: \${e.message}")
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.d("LocationService", "Service onStartCommand called")
        initSocketConnection()
        startTrackingPolling()
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
                        socket?.emit("join", username)
                    }

                    socket?.on("trackRequested") {
                        scope.launch {
                            val apiService = LocationApiService.create(token)
                            sendImmediateLocation(apiService, username)
                        }
                    }

                    socket?.connect()
                } catch (e: URISyntaxException) {
                    Log.e("LocationService", "Socket URI error", e)
                }
            }
        }
    }

    private fun startTrackingPolling() {
        scope.launch {
            while (isActive) {
                try {
                    val username = prefs.username.first()
                    val token = prefs.authToken.first()

                    if (username != null && username != "guest") {
                        val apiService = LocationApiService.create(token)
                        val status = apiService.checkFullStatus(username)

                        status.trackRequest?.let { request ->
                            if (request.active && request.timestamp > lastTrackRequestTimestamp) {
                                lastTrackRequestTimestamp = request.timestamp
                                sendImmediateLocation(apiService, username)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LocationService", "Tracking poll error: \${e.message}")
                }
                delay(POLLING_INTERVAL)
            }
        }
    }

    private fun sendImmediateLocation(apiService: LocationApiService, username: String) {
        locationHelper.getCurrentLocation().addOnSuccessListener { location ->
            location?.let {
                scope.launch {
                    try {
                        apiService.sendLocation(
                            username = username,
                            location = NetworkLocationRequest(
                                deviceId = "android_device_1",
                                latitude = it.latitude,
                                longitude = it.longitude
                            )
                        )
                    } catch (e: Exception) {
                        Log.e("LocationService", "Failed to send immediate location", e)
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return super.onBind(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        socket?.disconnect()
        socket?.off()
        scope.cancel()
        isRunning = false
    }
}
