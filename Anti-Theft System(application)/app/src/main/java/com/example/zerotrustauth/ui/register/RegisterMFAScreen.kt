package com.example.zerotrustauth.ui.register

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.zerotrustauth.ThemeManager
import com.example.zerotrustauth.ui.theme.*
import com.example.zerotrustauth.network.*
import kotlinx.coroutines.launch

@Composable
fun RegisterMFAScreen(
    username: String,
    onVerifySuccess: () -> Unit,
    onBack: () -> Unit
) {
    var otpCode by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var resendCooldown by remember { mutableIntStateOf(0) }
    
    val isDarkMode = ThemeManager.isDarkTheme.value
    val scope = rememberCoroutineScope()
    val apiService = remember { LocationApiService.create() }

    LaunchedEffect(resendCooldown) {
        if (resendCooldown > 0) {
            kotlinx.coroutines.delay(1000L)
            resendCooldown--
        }
    }

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
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
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
                    Text(text = "✉️", fontSize = 64.sp)
                    Text(
                        text = "XÁC MINH ĐĂNG KÝ",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isDarkMode) Color.White else Color.Black
                    )
                    Text(
                        text = "Mã xác minh đã được gửi đến email của bạn. Nhập mã để kích hoạt tài khoản: $username",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = if (isDarkMode) Color.LightGray else Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    if (errorMessage != null) {
                        Text(errorMessage!!, color = Color.Red, style = MaterialTheme.typography.bodySmall)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedTextField(
                        value = otpCode,
                        onValueChange = { 
                            if (it.length <= 6) otpCode = it 
                            errorMessage = null
                        },
                        label = { Text("Mã xác minh (6 số)") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        textStyle = LocalTextStyle.current.copy(
                            textAlign = TextAlign.Center,
                            fontSize = 24.sp,
                            letterSpacing = 8.sp
                        ),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isVerifying
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    TextButton(
                        onClick = {
                            if (resendCooldown == 0) {
                                scope.launch {
                                    try {
                                        val resp = apiService.resendOtp(ResendOtpRequest(username, "REGISTRATION"))
                                        if (resp.mockCode != null) {
                                            android.util.Log.d("MFA", "DEBUG OTP: ${resp.mockCode}")
                                        }
                                        resendCooldown = 30
                                        errorMessage = "Đã gửi lại mã!"
                                    } catch (e: Exception) {
                                        errorMessage = "Lỗi: ${e.message}"
                                    }
                                }
                            }
                        },
                        enabled = resendCooldown == 0 && !isVerifying
                    ) {
                        Text(
                            if (resendCooldown > 0) "Gửi lại sau ${resendCooldown}s" 
                            else "Chưa nhận được mã? Gửi lại",
                            color = if (isDarkMode) Color.Cyan else Color.Gray
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            if (otpCode.length == 6) {
                                isVerifying = true
                                scope.launch {
                                    try {
                                        apiService.verifyRegistration(VerifyOtpRequest(username, otpCode))
                                        isVerifying = false
                                        onVerifySuccess()
                                    } catch (e: Exception) {
                                        isVerifying = false
                                        errorMessage = "Lỗi xác minh: ${e.message}"
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isVerifying && otpCode.length == 6,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3B82F6)
                        )
                    ) {
                        if (isVerifying) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                        } else {
                            Text("KÍCH HOẠT", style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    TextButton(onClick = onBack, enabled = !isVerifying) {
                        Text("Quay lại", color = if (isDarkMode) Color.Cyan else Color.Gray)
                    }
                }
            }
        }
    }
}
