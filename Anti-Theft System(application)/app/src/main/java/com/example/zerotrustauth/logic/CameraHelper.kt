package com.example.zerotrustauth.logic

import android.content.Context
import android.util.Base64
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.example.zerotrustauth.data.SecurityPrefs
import com.example.zerotrustauth.network.LocationApiService
import com.example.zerotrustauth.network.LocationRequest
import com.example.zerotrustauth.network.IntruderRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

object CameraHelper {
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()

    fun captureAndUpload(context: Context, lifecycleOwner: LifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()

            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, imageCapture)

                val photoFile = File(context.cacheDir, "intruder_\${System.currentTimeMillis()}.jpg")
                val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

                imageCapture.takePicture(
                    outputOptions,
                    executor,
                    object : ImageCapture.OnImageSavedCallback {
                        override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                            Log.i("CameraHelper", "Intruder photo captured: \${photoFile.absolutePath}")
                            uploadIntruderData(context, photoFile)
                        }

                        override fun onError(exception: ImageCaptureException) {
                            Log.e("CameraHelper", "Capture failed: \${exception.message}", exception)
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e("CameraHelper", "Binding failed: \${e.message}")
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun uploadIntruderData(context: Context, file: File) {
        val scope = CoroutineScope(Dispatchers.IO)
        scope.launch {
            try {
                val prefs = SecurityPrefs(context)
                val username = prefs.username.first() ?: return@launch
                val token = prefs.authToken.first()
                
                // Read image and convert to Base64
                val bytes = file.readBytes()
                val base64Image = Base64.encodeToString(bytes, Base64.DEFAULT)
                
                // Get current location (Simplified helper)
                val locationHelper = LocationHelper(context)
                locationHelper.getCurrentLocation().addOnSuccessListener { location ->
                    val lat = location?.latitude ?: 0.0
                    val lon = location?.longitude ?: 0.0
                    
                    scope.launch {
                        try {
                            val apiService = LocationApiService.create(token)
                            apiService.uploadIntruderLog(
                                username = username,
                                request = IntruderRequest(
                                    imageBase64 = "data:image/jpeg;base64,\$base64Image",
                                    latitude = lat,
                                    longitude = lon
                                )
                            )
                            Log.i("CameraHelper", "Intruder data uploaded successfully for \$username")
                            file.delete() // Clean up
                        } catch (e: Exception) {
                            Log.e("CameraHelper", "Upload failed: \${e.message}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("CameraHelper", "Processing failed: \${e.message}")
            }
        }
    }
}
