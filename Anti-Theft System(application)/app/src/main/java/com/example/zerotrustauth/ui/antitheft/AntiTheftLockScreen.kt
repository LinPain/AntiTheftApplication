package com.example.zerotrustauth.ui.antitheft

import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import com.example.zerotrustauth.data.SecurityPrefs
import kotlinx.coroutines.launch
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Safely find the FragmentActivity from the context
 */
fun Context.findActivity(): FragmentActivity? = when (this) {
    is FragmentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
fun AntiTheftLockScreen(
    onUnlockSuccess: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val securityPrefs = remember { SecurityPrefs(context) }
    val correctPin = "1234" 

    // Hardware checks
    val biometricManager = remember { BiometricManager.from(context) }
    val canUseBiometric = remember {
        biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS
    }

    // Biometric setup
    val executor = remember { ContextCompat.getMainExecutor(context) }
    val biometricPrompt = remember {
        val activity = context.findActivity()
        if (activity != null) {
            BiometricPrompt(activity, executor,
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        scope.launch {
                            securityPrefs.setRemoteLockdown(false)
                            onUnlockSuccess()
                        }
                    }
                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        // Ignore cancellation by user
                        if (errorCode != BiometricPrompt.ERROR_USER_CANCELED && errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                            error = "Lỗi sinh trắc học: $errString"
                        }
                    }
                })
        } else null
    }

    val promptInfo = remember {
        BiometricPrompt.PromptInfo.Builder()
            .setTitle("Xác thực chủ sở hữu")
            .setSubtitle("Sử dụng khuôn mặt hoặc vân tay để mở khoá")
            .setNegativeButtonText("Sử dụng mã PIN")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            .build()
    }

    // Prevent back navigation while locked
    BackHandler(enabled = true) { }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)) 
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = Color(0xFFE53935)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "KHOÁ CHỐNG TRỘM",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Text(
            text = "Hệ thống đã tự động khoá ứng dụng để bảo vệ dữ liệu của bạn.",
            textAlign = TextAlign.Center,
            color = Color.Gray,
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        OutlinedTextField(
            value = pin,
            onValueChange = { 
                if (it.length <= 4) pin = it 
                error = null
            },
            label = { Text("Nhập mã PIN bảo mật", color = Color.White) },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFFE53935),
                unfocusedBorderColor = Color.Gray
            ),
            isError = error != null
        )

        if (error != null) {
            Text(
                text = error!!,
                color = Color(0xFFE53935),
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = {
                if (pin == correctPin) {
                    scope.launch {
                        securityPrefs.setRemoteLockdown(false)
                        onUnlockSuccess()
                    }
                } else {
                    error = "Mã PIN không chính xác"
                    pin = ""
                }
            },
            modifier = Modifier.fillMaxWidth().height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935))
        ) {
            Icon(Icons.Default.Lock, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("MỞ KHOÁ BẰNG PIN", fontWeight = FontWeight.Bold)
        }

        if (canUseBiometric && biometricPrompt != null) {
            Spacer(modifier = Modifier.height(16.dp))

            OutlinedButton(
                onClick = {
                    try {
                        biometricPrompt.authenticate(promptInfo)
                    } catch (e: Exception) {
                        error = "Thiết bị không hỗ trợ sinh trắc học"
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.Gray)
            ) {
                Icon(Icons.Default.Fingerprint, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SỬ DỤNG FACE ID / VÂN TAY", fontWeight = FontWeight.Bold)
            }
        }
    }
}
