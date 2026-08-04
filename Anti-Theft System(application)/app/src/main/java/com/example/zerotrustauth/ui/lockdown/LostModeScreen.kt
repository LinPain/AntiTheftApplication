package com.example.zerotrustauth.ui.lockdown

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.BackHandler
import com.example.zerotrustauth.ui.components.QrCodeView
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@Composable
fun LostModeScreen(
    message: String,
    phoneNumber: String,
    ownerName: String = "",
    ownerEmail: String = ""
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showUnlockDialog by remember { mutableStateOf(false) }

    // Prevent back navigation
    BackHandler(enabled = true) { }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE53935)) // High-contrast Red
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            shape = CircleShape,
            color = Color.White.copy(alpha = 0.2f),
            modifier = Modifier.size(120.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "THIS DEVICE IS LOST",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Black,
                    textAlign = TextAlign.Center
                )

                if (ownerName.isNotBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Chủ sở hữu:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        text = ownerName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                }

                if (phoneNumber.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Vui lòng liên hệ:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        text = phoneNumber,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE53935)
                    )
                }

                if (ownerEmail.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Email:",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.Gray
                    )
                    Text(
                        text = ownerEmail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Add QR Code for quick contact
                val contactInfo = "TEL:$phoneNumber\nEMAIL:$ownerEmail\nNAME:$ownerName"
                QrCodeView(content = contactInfo)
                
                Text(
                    text = "Quét để liên hệ chủ sở hữu",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(64.dp))

        if (phoneNumber.isNotEmpty()) {
            Button(
                onClick = {
                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phoneNumber"))
                    context.startActivity(intent)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp),
                shape = RoundedCornerShape(32.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Icon(Icons.Default.Call, contentDescription = null, tint = Color(0xFFE53935))
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "GỌI CHO CHỦ SỞ HỮU",
                    color = Color(0xFFE53935),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Normal usage is disabled by remote administrator.",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        TextButton(onClick = { showUnlockDialog = true }) {
            Text("XÁC THỰC CHỦ SỞ HỮU", color = Color.White.copy(alpha = 0.5f))
        }
    }

    if (showUnlockDialog) {
        AlertDialog(
            onDismissRequest = { showUnlockDialog = false },
            title = { Text("Xác thực chủ sở hữu") },
            text = {
                var pin by remember { mutableStateOf("") }
                var error by remember { mutableStateOf<String?>(null) }
                
                Column {
                    Text("Nhập mã PIN bảo mật của bạn để mở khoá thiết bị.", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = pin,
                        onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pin = it },
                        label = { Text("Mã PIN 4 số") },
                        visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (error != null) {
                        Text(error!!, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                    }
                    
                    Spacer(Modifier.height(16.dp))
                    
                    Button(
                        onClick = {
                            scope.launch {
                                val prefs = com.example.zerotrustauth.data.SecurityPrefs(context)
                                val correctPin = prefs.localPin.first()
                                if (pin == correctPin) {
                                    // 1. Clear local locks
                                    prefs.clearAllLocks()

                                    // 2. IMPORTANT: Update backend so it doesn't re-lock us on next sync
                                    val token = prefs.authToken.first()
                                    val username = prefs.username.first()?.lowercase()?.trim()
                                    if (token != null && username != null) {
                                        try {
                                            val api = com.example.zerotrustauth.network.LocationApiService.create(token)
                                            // Tell server the owner manually unlocked
                                            api.notifyGenericAlert(username, com.example.zerotrustauth.network.GenericAlertRequest("OWNER_UNLOCKED_DEVICE", emptyMap()))
                                            
                                            // Disable Lost Mode on server
                                            api.setLostMode(username, com.example.zerotrustauth.network.LostModeRequest(active = false))
                                        } catch (e: Exception) {
                                            android.util.Log.e("LostMode", "Failed to sync unlock to server: ${e.message}")
                                        }
                                    }

                                    showUnlockDialog = false
                                } else {
                                    error = "Mã PIN không chính xác"
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = pin.length == 4
                    ) {
                        Text("XÁC NHẬN MỞ KHOÁ")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showUnlockDialog = false }) {
                    Text("HUỶ")
                }
            }
        )
    }
}
