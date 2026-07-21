package com.example.zerotrustauth.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import com.example.zerotrustauth.network.LocationApiService
import com.example.zerotrustauth.network.LocationRequest as NetworkLocationRequest
import com.google.android.gms.location.*
import com.example.zerotrustauth.data.SecurityPrefs
import com.example.zerotrustauth.logic.LocationHelper
import androidx.lifecycle.LifecycleService
import com.example.zerotrustauth.logic.CameraHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * Optimized Location Service
 * Purely handles GPS capture and geofencing. 
 */
class LocationService : LifecycleService() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var prefs: SecurityPrefs
    private lateinit var locationHelper: LocationHelper
    private var isTrackingUpdatesActive = false

    companion object {
        private const val NOTIFICATION_ID = 1234
        private const val CHANNEL_ID = "location_channel"
        var isRunning = false
        
        private var instance: LocationService? = null
        
        fun triggerIntruderCapture() {
            instance?.let { 
                Log.i("LocationService", "Triggering intruder capture")
                CameraHelper.captureAndUpload(it, it) 
            }
        }

        /**
         * Wake up tracking and upload location immediately
         */
        fun triggerImmediateUpload(context: android.content.Context) {
            // Force start service if not running
            if (!isRunning) {
                Log.i("LocationService", "Waking up service for remote track request")
                val intent = Intent(context, LocationService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            }

            instance?.let {
                Log.i("LocationService", "Triggering immediate location pulse")
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
        isRunning = true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (!isTrackingUpdatesActive) {
            startLocationUpdates()
        }
        return START_STICKY
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e("LocationService", "Cannot start foreground service: Location permission not granted")
            stopSelf()
            return
        }

        // On Android 14+, we must also check for FOREGROUND_SERVICE_LOCATION
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.FOREGROUND_SERVICE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.e("LocationService", "Cannot start foreground service: FOREGROUND_SERVICE_LOCATION permission not granted")
                stopSelf()
                return
            }
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).apply {
            setMinUpdateIntervalMillis(2000L)
        }.build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                for (location in locationResult.locations) {
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
        isTrackingUpdatesActive = true
    }

    private fun checkGeofence(lat: Double, lon: Double) {
        scope.launch {
            val safeLat = prefs.safeZoneLat.first()
            val safeLon = prefs.safeZoneLon.first()
            val radius = prefs.safeZoneRadius.first()

            if (safeLat != null && safeLon != null) {
                val results = FloatArray(1)
                android.location.Location.distanceBetween(lat, lon, safeLat, safeLon, results)
                val isOutside = results[0] > radius
                
                // Set the real geofence flag instead of incrementing failed PINs
                prefs.setOutsideSafeZone(isOutside)
                
                if (isOutside) {
                    Log.w("LocationService", "OUTSIDE SAFE ZONE! Dist: ${results[0]}m")
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
                val username = prefs.username.first()
                if (username == null || username == "guest" || token.isNullOrBlank()) return@launch
                
                LocationApiService.create(token).sendLocation(
                    username = username,
                    location = NetworkLocationRequest(locationHelper.getDeviceId(), lat, lon)
                )
                Log.d("LocationService", "GPS Upload success: $lat, $lon")
            } catch (e: Exception) {
                Log.e("LocationService", "GPS Upload failed: ${e.message}")
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
                            location = NetworkLocationRequest(locationHelper.getDeviceId(), it.latitude, it.longitude)
                        )
                        Log.d("LocationService", "Immediate GPS pulse success")
                    } catch (e: Exception) {
                        Log.e("LocationService", "Immediate pulse failed")
                    }
                }
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? = super.onBind(intent)

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        scope.cancel()
        isRunning = false
        isTrackingUpdatesActive = false
        instance = null
    }
}
