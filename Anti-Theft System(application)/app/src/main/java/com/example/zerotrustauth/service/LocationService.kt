package com.example.zerotrustauth.service

import android.annotation.SuppressLint
import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.Looper
import android.os.BatteryManager
import android.util.Log
import android.Manifest
import android.content.IntentFilter
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import com.example.zerotrustauth.network.LocationApiService
import com.example.zerotrustauth.network.LocationRequest as NetworkLocationRequest
import com.example.zerotrustauth.network.GenericAlertRequest
import com.google.android.gms.location.*
import com.example.zerotrustauth.data.SecurityPrefs
import com.example.zerotrustauth.logic.LocationHelper
import com.example.zerotrustauth.logic.RiskEngine
import androidx.lifecycle.LifecycleService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first

/**
 * Optimized Location Service
 * Purely handles GPS capture and geofencing without local noise.
 */
class LocationService : LifecycleService() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private lateinit var prefs: SecurityPrefs
    private lateinit var locationHelper: LocationHelper
    private var isTrackingUpdatesActive = false
    private var lastBatteryLevel = -1
    private var isFirstConnection = true

    companion object {
        private const val NOTIFICATION_ID = 1234
        private const val CHANNEL_ID = "location_channel_silent"
        var isRunning = false
        
        private var instance: LocationService? = null
        
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
        
        // Dynamic Interval Management
        scope.launch {
            prefs.isLostModeActive.collect { isLost ->
                if (isTrackingUpdatesActive) {
                    withContext(Dispatchers.Main) {
                        startLocationUpdates(isLost)
                    }
                }
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (!isTrackingUpdatesActive) {
            scope.launch {
                val isLost = prefs.isLostModeActive.first()
                withContext(Dispatchers.Main) {
                    startLocationUpdates(isLost)
                }
            }
            // Send initial discovery pulse
            scope.launch {
                val token = prefs.authToken.first()
                val username = prefs.username.first()
                if (username != null && username != "guest" && token != null) {
                    sendImmediateLocation(LocationApiService.create(token), username)
                }
            }
        }
        return START_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Hệ thống Bảo mật",
                NotificationManager.IMPORTANCE_MIN
            ).apply {
                description = "Giám sát bảo mật thiết bị"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates(isLostMode: Boolean = false) {
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

        val interval = if (isLostMode) 30000L else 300000L
        val minInterval = if (isLostMode) 15000L else 60000L

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval).apply {
            setMinUpdateIntervalMillis(minInterval)
            setMaxUpdateDelayMillis(interval * 2)
        }.build()

        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    sendLocationToBackend(location)
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
            .setContentTitle("Thiết bị đang được bảo vệ")
            .setContentText("Hệ thống Zero Trust đang hoạt động")
            .setSmallIcon(android.R.drawable.ic_lock_idle_lock)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            .setOngoing(true)
            .setSilent(true)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        isTrackingUpdatesActive = true
    }

    private fun checkBattery(username: String, apiService: LocationApiService) {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            applicationContext.registerReceiver(null, filter)
        }
        val level: Int = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        
        // Alert if battery drops below 15% and we haven't alerted for this level recently
        if (level in 1..15 && level != lastBatteryLevel) {
            lastBatteryLevel = level
            scope.launch {
                try {
                    apiService.notifyGenericAlert(username, GenericAlertRequest("LOW_BATTERY", mapOf(
                        "deviceName" to locationHelper.getDeviceName(),
                        "batteryLevel" to level
                    )))
                } catch (e: Exception) {
                    Log.e("LocationService", "Battery alert failed")
                }
            }
        } else if (level > 20) {
            lastBatteryLevel = -1 // Reset alert trigger when battery is charged
        }
    }

    private fun checkGeofence(lat: Double, lon: Double) {
        scope.launch {
            val username = prefs.username.first()?.trim()?.lowercase() ?: return@launch
            val safeLat = prefs.safeZoneLat.first()
            val safeLon = prefs.safeZoneLon.first()
            val radius = prefs.safeZoneRadius.first()

            if (safeLat != null && safeLon != null) {
                val results = FloatArray(1)
                android.location.Location.distanceBetween(lat, lon, safeLat, safeLon, results)
                val isOutside = results[0] > radius
                
                val currentlyOutside = prefs.isOutsideSafeZone.first()
                if (isOutside != currentlyOutside) {
                    prefs.setOutsideSafeZone(isOutside)
                    
                    if (isOutside) {
                        Log.w("LocationService", "OUTSIDE SAFE ZONE! Dist: ${results[0]}m")
                        val token = prefs.authToken.first()
                        if (token != null) {
                            val apiService = LocationApiService.create(token)
                            apiService.notifyGenericAlert(username, GenericAlertRequest("SIGNIFICANT_LOCATION_CHANGE", mapOf(
                                "latitude" to lat,
                                "longitude" to lon,
                                "distance" to results[0]
                            )))
                        }
                    }
                }
            }
        }
    }

    private fun sendLocationToBackend(location: android.location.Location) {
        scope.launch {
            try {
                val token = prefs.authToken.first()
                val username = prefs.username.first()
                if (username == null || username == "guest" || token.isNullOrBlank()) {
                    Log.w("LocationService", "Skipping GPS upload: User not logged in (username: $username)")
                    return@launch
                }
                
                val cleanUsername = username.trim().lowercase()
                val apiService = LocationApiService.create(token)
                
                if (isFirstConnection) {
                    isFirstConnection = false
                    apiService.notifyGenericAlert(cleanUsername, GenericAlertRequest("DEVICE_RECONNECTED", mapOf("deviceName" to locationHelper.getDeviceName())))
                }
                
                checkBattery(cleanUsername, apiService)

                val riskScore = RiskEngine.calculateRiskScore(
                    isTrustedDevice = true,
                    isOutsideSafeZone = prefs.isOutsideSafeZone.first(),
                    failedUnlockAttempts = prefs.failedUnlockCount.first()
                )

                apiService.sendLocation(
                    username = cleanUsername,
                    location = NetworkLocationRequest(
                        deviceId = locationHelper.getDeviceId(),
                        deviceName = locationHelper.getDeviceName(),
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy,
                        speed = location.speed,
                        timestamp = location.time,
                        batteryLevel = getBatteryLevel(),
                        isCharging = isBatteryCharging(),
                        manufacturer = android.os.Build.MANUFACTURER,
                        model = android.os.Build.MODEL,
                        androidVersion = android.os.Build.VERSION.RELEASE,
                        apiLevel = android.os.Build.VERSION.SDK_INT,
                        riskScore = riskScore
                    )
                )
                Log.d("LocationService", "GPS Telemetry success for $cleanUsername")
            } catch (e: Exception) {
                Log.e("LocationService", "GPS Telemetry failed: ${e.message}")
            }
        }
    }

    private fun getBatteryLevel(): Int {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            applicationContext.registerReceiver(null, filter)
        }
        return batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    }

    private fun isBatteryCharging(): Boolean {
        val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
            applicationContext.registerReceiver(null, filter)
        }
        val status = batteryStatus?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        return status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun sendImmediateLocation(apiService: LocationApiService, username: String) {
        // 1. Send immediate Discovery Pulse (0,0) to register device presence on the server list
        scope.launch {
            try {
                val cleanUsername = username.trim().lowercase()
                apiService.sendLocation(
                    username = cleanUsername,
                    location = NetworkLocationRequest(
                        deviceId = locationHelper.getDeviceId(),
                        deviceName = locationHelper.getDeviceName(),
                        latitude = 0.0,
                        longitude = 0.0
                    )
                )
                Log.d("LocationService", "Discovery pulse sent (0,0) for $cleanUsername")
            } catch (e: Exception) {
                Log.e("LocationService", "Discovery pulse failed")
            }
        }

        // 2. Attempt high-accuracy GPS capture
        locationHelper.getCurrentLocation().addOnSuccessListener { location ->
            location?.let {
                scope.launch {
                    try {
                        val cleanUsername = username.trim().lowercase()
                        apiService.sendLocation(
                            username = cleanUsername,
                            location = NetworkLocationRequest(
                                deviceId = locationHelper.getDeviceId(),
                                deviceName = locationHelper.getDeviceName(),
                                latitude = it.latitude,
                                longitude = it.longitude,
                                accuracy = it.accuracy,
                                speed = it.speed,
                                timestamp = it.time,
                                batteryLevel = getBatteryLevel(),
                                isCharging = isBatteryCharging(),
                                manufacturer = android.os.Build.MANUFACTURER,
                                model = android.os.Build.MODEL,
                                androidVersion = android.os.Build.VERSION.RELEASE,
                                apiLevel = android.os.Build.VERSION.SDK_INT
                            )
                        )
                        Log.d("LocationService", "Real GPS location uploaded for $cleanUsername: ${it.latitude}, ${it.longitude}")
                    } catch (e: Exception) {
                        Log.e("LocationService", "Real GPS upload failed")
                    }
                }
            } ?: run {
                Log.w("LocationService", "GPS returned null, device remains registered via pulse")
            }
        }.addOnFailureListener {
            Log.e("LocationService", "GPS capture failed: ${it.message}")
        }
    }

    override fun onBind(intent: Intent): IBinder? = super.onBind(intent)

    override fun onDestroy() {
        super.onDestroy()
        if (::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
        scope.cancel()
        isRunning = false
        isTrackingUpdatesActive = false
        instance = null
    }
}
