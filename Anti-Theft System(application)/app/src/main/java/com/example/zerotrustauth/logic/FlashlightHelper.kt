package com.example.zerotrustauth.logic

import android.content.Context
import android.hardware.camera2.CameraManager
import android.util.Log
import kotlinx.coroutines.*

object FlashlightHelper {
    private var flashJob: Job? = null

    fun toggleFlash(context: Context, active: Boolean) {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = cameraManager.cameraIdList.getOrNull(0) ?: return
            
            if (active) {
                // Blink effect
                flashJob?.cancel()
                flashJob = CoroutineScope(Dispatchers.Default).launch {
                    while (isActive) {
                        cameraManager.setTorchMode(cameraId, true)
                        delay(500)
                        cameraManager.setTorchMode(cameraId, false)
                        delay(500)
                    }
                }
            } else {
                flashJob?.cancel()
                cameraManager.setTorchMode(cameraId, false)
            }
        } catch (e: Exception) {
            Log.e("FlashlightHelper", "Flash error: ${e.message}")
        }
    }
}
