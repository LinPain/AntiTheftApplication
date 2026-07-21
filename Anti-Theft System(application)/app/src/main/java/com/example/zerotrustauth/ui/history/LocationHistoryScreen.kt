package com.example.zerotrustauth.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.zerotrustauth.data.SecurityPrefs
import androidx.compose.runtime.collectAsState
import com.example.zerotrustauth.logic.LocationHelper
import com.example.zerotrustauth.network.LocationApiService
import com.example.zerotrustauth.network.LocationResponse
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocationHistoryScreen(
    onBack: () -> Unit,
    onNavigateToMap: (Double, Double) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val securityPrefs = remember { SecurityPrefs(context) }
    val username = securityPrefs.username.collectAsState(initial = "guest").value ?: "guest"
    val authToken = securityPrefs.authToken.collectAsState(initial = null).value

    val apiService = remember(authToken) { LocationApiService.create(authToken) }
    val locationHelper = remember { LocationHelper(context) }
    var history by remember { mutableStateOf<List<LocationResponse>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(username) {
        try {
            history = apiService.getLocationHistory(username, locationHelper.getDeviceId())
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lịch sử vị trí") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (history.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.outline)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Không có lịch sử vị trí", style = MaterialTheme.typography.bodyLarge)
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(history) { item ->
                        LocationHistoryItem(item, onClick = {
                            onNavigateToMap(item.latitude, item.longitude)
                        })
                    }
                }
            }
        }
    }
}

@Composable
fun LocationHistoryItem(item: LocationResponse, onClick: () -> Unit) {
    val formattedTime = try {
        val zonedDateTime = ZonedDateTime.parse(item.timestamp)
        zonedDateTime.format(DateTimeFormatter.ofPattern("HH:mm:ss, dd/MM/yyyy"))
    } catch (e: Exception) {
        item.timestamp
    }

    ListItem(
        modifier = Modifier.clickable { onClick() },
        headlineContent = { Text("Tọa độ: ${item.latitude}, ${item.longitude}") },
        supportingContent = { Text(formattedTime) },
        leadingContent = {
            Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
    )
    HorizontalDivider()
}
