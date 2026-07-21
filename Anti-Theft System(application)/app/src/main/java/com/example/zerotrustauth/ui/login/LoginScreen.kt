package com.example.zerotrustauth.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import com.example.zerotrustauth.ThemeManager
import com.example.zerotrustauth.ui.theme.*
import com.example.zerotrustauth.data.SecurityPrefs
import androidx.compose.runtime.collectAsState
import com.example.zerotrustauth.logic.RiskEngine
import com.example.zerotrustauth.network.LocationApiService
import com.example.zerotrustauth.network.LoginRequest
import kotlinx.coroutines.launch

@Composable
fun LoginScreen(
    onNavigateToRegister: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onLoginSuccess: (String) -> Unit,
    onNavigateToMFA: (String) -> Unit,
    onNavigateToLockdown: () -> Unit,
    onNavigateToVerification: (String) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var rememberMe by remember { mutableStateOf(false) }
    var showPinSetup by remember { mutableStateOf(false) }
    var pendingSuccessData by remember { mutableStateOf<String?>(null) }
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val securityPrefs = remember { SecurityPrefs(context) }
    val failedUnlockCount = securityPrefs.failedUnlockCount.collectAsState(initial = 0).value
    val isOutsideSafeZone = securityPrefs.isOutsideSafeZone.collectAsState(initial = false).value
    
    val isDarkMode = ThemeManager.isDarkTheme.value
    val scope = rememberCoroutineScope()
    val apiService = remember { LocationApiService.create() }

    val backgroundGradient = if (isDarkMode) {
        Brush.verticalGradient(
            colors = listOf(Color(0xFF020617), Color(0xFF0F172A), Color(0xFF1E293B))
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(Color(0xFFF8FAFC), Color(0xFFE2E8F0))
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(backgroundGradient)) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = { ThemeManager.isDarkTheme.value = !isDarkMode }) {
                    Icon(
                        imageVector = if (isDarkMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                        contentDescription = "Theme",
                        tint = if (isDarkMode) Color.White else Color.Black
                    )
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth().align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = if (isDarkMode) Color(0xFF1E293B).copy(alpha = 0.95f) else Color.White
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "🔐", fontSize = 64.sp)
                        Text(
                            text = "ANTI-THEFT SYSTEM",
                            style = MaterialTheme.typography.headlineSmall,
                            color = if (isDarkMode) Color.White else Color.Black
                        )
                        
                        Spacer(modifier = Modifier.height(32.dp))

                        if (errorMessage != null) {
                            Text(errorMessage!!, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        OutlinedTextField(
                            value = username,
                            onValueChange = { username = it; errorMessage = null },
                            label = { Text("Username hoặc Email") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            enabled = !isLoading
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; errorMessage = null },
                            label = { Text("Mật khẩu") },
                            visualTransformation = PasswordVisualTransformation(),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true,
                            enabled = !isLoading
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))

                        TextButton(
                            onClick = onNavigateToForgotPassword,
                            enabled = !isLoading,
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Quên mật khẩu?", color = if (isDarkMode) Color.LightGray else Color.Gray)
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                        ) {
                            Checkbox(
                                checked = rememberMe,
                                onCheckedChange = { rememberMe = it },
                                enabled = !isLoading
                            )
                            Text(
                                "Ghi nhớ tài khoản",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDarkMode) Color.White else Color.Black,
                                modifier = Modifier.clickable { rememberMe = !rememberMe }
                            )
                        }

                        Button(
                            onClick = { 
                                if (username.isBlank() || password.isBlank()) {
                                    errorMessage = "Vui lòng nhập đầy đủ"
                                    return@Button
                                }
                                isLoading = true
                                scope.launch {
                                    try {
                                        val riskScore = RiskEngine.calculateRiskScore(
                                            isTrustedDevice = true, 
                                            isOutsideSafeZone = isOutsideSafeZone,
                                            failedUnlockAttempts = failedUnlockCount
                                        )

                                        val response = apiService.login(LoginRequest(username.trim().lowercase(), password, riskScore))
                                        isLoading = false
                                        
                                        if (response.lockdownRequired) {
                                            onNavigateToLockdown()
                                        } else if (response.verificationRequired) {
                                            onNavigateToVerification(response.username ?: username)
                                        } else if (response.mfaRequired) {
                                            onNavigateToMFA(response.username ?: username)
                                        } else {
                                            securityPrefs.saveAuthData(response.token, response.username ?: username)
                                            securityPrefs.setRememberMe(rememberMe)
                                            
                                            if (rememberMe) {
                                                pendingSuccessData = response.username ?: username
                                                showPinSetup = true
                                            } else {
                                                securityPrefs.setLocalPin(null)
                                                onLoginSuccess(response.username ?: username)
                                            }
                                        }
                                    } catch (e: Exception) {
                                        isLoading = false
                                        errorMessage = "Đăng nhập thất bại: ${e.message}"
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(56.dp),
                            shape = RoundedCornerShape(16.dp),
                            enabled = !isLoading,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDarkMode) Color(0xFF3B82F6) else NavyPrimary
                            )
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                            } else {
                                Text("ĐĂNG NHẬP", style = MaterialTheme.typography.titleMedium)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        TextButton(onClick = onNavigateToRegister, enabled = !isLoading) {
                            Text("Chưa có tài khoản? Đăng ký", color = if (isDarkMode) Color.Cyan else NavyPrimary)
                        }
                    }
                }
            }
        }

        if (showPinSetup) {
            PinSetupDialog(
                onDismiss = { 
                    showPinSetup = false
                    pendingSuccessData?.let { onLoginSuccess(it) }
                },
                onPinSet = { pin ->
                    scope.launch {
                        securityPrefs.setLocalPin(pin)
                        showPinSetup = false
                        pendingSuccessData?.let { onLoginSuccess(it) }
                    }
                }
            )
        }
    }
}
