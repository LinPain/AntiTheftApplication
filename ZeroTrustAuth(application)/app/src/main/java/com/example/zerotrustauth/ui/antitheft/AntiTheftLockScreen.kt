package com.example.zerotrustauth.ui.antitheft

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
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
import com.example.zerotrustauth.logic.WipeManager
import androidx.compose.ui.platform.LocalContext

@Composable
fun AntiTheftLockScreen(
    onUnlockSuccess: () -> Unit
) {
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val correctPin = "1234" // In a real app, this would be stored securely in encrypted prefs

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF1A1A1A)) // Dark security theme
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
                    onUnlockSuccess()
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
            Text("MỞ KHOÁ ỨNG DỤNG", fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(64.dp))

        TextButton(
            onClick = { WipeManager.performSecureWipe(context) }
        ) {
            Text("XÓA DỮ LIỆU KHẨN CẤP", color = Color(0xFFE53935))
        }
    }
}
