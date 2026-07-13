package com.example.zerotrustauth.ui.location

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.example.zerotrustauth.data.SecurityPrefs
import androidx.compose.runtime.collectAsState
import com.example.zerotrustauth.logic.LocationHelper
import com.example.zerotrustauth.network.LocationApiService
import com.example.zerotrustauth.network.LocationRequest
import com.example.zerotrustauth.service.LocationService
import kotlinx.coroutines.launch
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationTrackingScreen(
    onBack: () -> Unit,
    initialLat: Double? = null,
    initialLon: Double? = null
) {
    val context = LocalContext.current
    val securityPrefs = remember { SecurityPrefs(context) }
    val username = securityPrefs.username.collectAsState(initial = "guest").value ?: "guest"
    val authToken = securityPrefs.authToken.collectAsState(initial = null).value

    val scope = rememberCoroutineScope()
    val locationHelper = remember { LocationHelper(context) }
    val apiService = remember(authToken) { LocationApiService.create(authToken) }

    // Initialize OSMDroid configuration
    LaunchedEffect(Unit) {
        Configuration.getInstance().load(context, context.getSharedPreferences("osm_pref", 0))
        Configuration.getInstance().userAgentValue = context.packageName
    }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { isGranted ->
            hasLocationPermission = isGranted
        }
    )

    val notificationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* Handled by system */ }
    )

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    var currentLocation by remember { 
        mutableStateOf(
            if (initialLat != null && initialLon != null) GeoPoint(initialLat, initialLon) else null
        )
    }
    
    val isLiveTrackingStored = securityPrefs.isLiveTrackingEnabled.collectAsState(initial = false).value
    var isRealTimeTrackingEnabled by remember(isLiveTrackingStored) { mutableStateOf(isLiveTrackingStored) }

    // We use a key to trigger updates in the AndroidView
    var mapUpdateTrigger by remember { mutableLongStateOf(0L) }

    // Periodic Location Updates for UI (Real-time Follow)
    LaunchedEffect(hasLocationPermission, isRealTimeTrackingEnabled) {
        if (hasLocationPermission && initialLat == null) {
            while (true) {
                locationHelper.getCurrentLocation().addOnSuccessListener { location ->
                    location?.let {
                        val geoPoint = GeoPoint(it.latitude, it.longitude)
                        currentLocation = geoPoint
                        mapUpdateTrigger++
                        
                        // Send to backend only if live tracking is OFF in the background service
                        if (!isRealTimeTrackingEnabled) {
                            scope.launch {
                                try {
                                    android.util.Log.d("LocationTrackingUI", "Sending UI-triggered location update")
                                    apiService.sendLocation(
                                        username = username,
                                        location = LocationRequest(
                                            deviceId = "android_device_1",
                                            latitude = it.latitude,
                                            longitude = it.longitude
                                        )
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }
                    }
                }
                kotlinx.coroutines.delay(5000) // Update UI every 5 seconds
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (initialLat != null) "Xem vị trí cũ" else "Theo dõi vị trí (OSM)") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (initialLat == null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Live", style = MaterialTheme.typography.labelSmall)
                            Switch(
                                checked = isRealTimeTrackingEnabled,
                                onCheckedChange = { enabled ->
                                    isRealTimeTrackingEnabled = enabled
                                    scope.launch { securityPrefs.setLiveTracking(enabled) }

                                    val intent = Intent(context, LocationService::class.java)
                                    if (enabled) {
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                            context.startForegroundService(intent)
                                        } else {
                                            context.startService(intent)
                                        }
                                    } else {
                                        context.stopService(intent)
                                    }
                                },
                                modifier = Modifier.scale(0.7f)
                            )
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (hasLocationPermission || initialLat != null) {
                AndroidView(
                    factory = { ctx ->
                        MapView(ctx).apply {
                            setTileSource(TileSourceFactory.MAPNIK)
                            setMultiTouchControls(true)
                            controller.setZoom(if (initialLat != null) 18.0 else 15.0)
                        }
                    },
                    update = { mapView ->
                        currentLocation?.let { geoPoint ->
                            // Auto-center with animation to "follow" the device
                            mapView.controller.animateTo(geoPoint)
                            
                            // Clear existing markers and add a new one
                            mapView.overlays.clear()
                            val marker = Marker(mapView)
                            marker.position = geoPoint
                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
                            marker.title = if (initialLat != null) "Vị trí đã chọn" else "Vị trí hiện tại"
                            mapView.overlays.add(marker)
                            
                            mapView.invalidate()
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Current Location Button (Bottom Left)
                SmallFloatingActionButton(
                    onClick = {
                        locationHelper.getCurrentLocation().addOnSuccessListener { location ->
                            location?.let {
                                currentLocation = GeoPoint(it.latitude, it.longitude)
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Center on current location")
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Cần quyền truy cập vị trí để sử dụng tính năng này.")
                    Button(onClick = { launcher.launch(Manifest.permission.ACCESS_FINE_LOCATION) }) {
                        Text("Cấp quyền")
                    }
                }
            }
        }
    }
}
