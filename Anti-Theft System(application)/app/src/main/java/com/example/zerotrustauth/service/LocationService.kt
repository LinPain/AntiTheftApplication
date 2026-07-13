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
import io.socket.client.IO
import io.socket.client.Socket
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import java.net.URISyntaxException

class LocationService : Service() {

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
        private const val POLLING_INTERVAL = 5000L // 5 seconds fallback
        var isRunning = false
    }

    override fun onCreate() {
        super.onCreate()
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
                    Log.d("LocationService", "New location: ${location.latitude}, ${location.longitude}")
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
                    Log.w("LocationService", "OUTSIDE SAFE ZONE! Distance: $distance m")
                    // Increase risk score slightly
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
            .setContentText("Gửi lúc $time: $lat, $lon")
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
                    Log.w("LocationService", "Skipping location update: User not logged in or token missing")
                    return@launch
                }
                
                Log.i("LocationService", "Sending location for $username: $lat, $lon")
                val apiService = LocationApiService.create(token)

                apiService.sendLocation(
                    username = username,
                    location = NetworkLocationRequest(
                        deviceId = "android_device_1",
                        latitude = lat,
                        longitude = lon
                    )
                )
                Log.d("LocationService", "Location update sent successfully for $username")
            } catch (e: Exception) {
                Log.e("LocationService", "Error sending location to backend: ${e.message}", e)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("LocationService", "Service onStartCommand called")
        initSocketConnection()
        startTrackingPolling()
        // Ensure service is started as sticky so it's revived by the OS if killed
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
                        Log.i("LocationService", "Socket connected! Joining room: $username")
                        socket?.emit("join", username)
                    }

                    socket?.on("trackRequested") { args ->
                        Log.i("LocationService", "REAL-TIME Track Request received!")
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

                        // Handle Immediate Track Request Fallback
                        status.trackRequest?.let { request ->
                            if (request.active && request.timestamp > lastTrackRequestTimestamp) {
                                lastTrackRequestTimestamp = request.timestamp
                                sendImmediateLocation(apiService, username)
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LocationService", "Tracking poll error: ${e.message}")
                }
                delay(POLLING_INTERVAL)
            }
        }
    }

    private fun sendImmediateLocation(apiService: LocationApiService, username: String) {
        Log.i("LocationService", "Processing immediate track request")
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
                        Log.d("LocationService", "Immediate location sent")
                    } catch (e: Exception) {
                        Log.e("LocationService", "Failed to send immediate location", e)
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        socket?.disconnect()
        socket?.off()
        scope.cancel()
        isRunning = false
    }
}
