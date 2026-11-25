package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.media.ImageStorageManager

@Composable
fun CameraCaptureScreen(
    imageStorageManager: ImageStorageManager,
    onImageCaptured: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var flashEnabled by remember { mutableStateOf(false) }
    var captureError by remember { mutableStateOf<String?>(null) }
    var isCapturing by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    if (!hasCameraPermission) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(56.dp)
                )
                Text(
                    text = "يلزم منح إذن الكاميرا لالتقاط صور الرموز والنقوش مباشرة.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    modifier = Modifier.testTag("request_camera_permission_button")
                ) {
                    Text("منح إذن الكاميرا")
                }
                Button(onClick = onClose) {
                    Text("العودة")
                }
            }
        }
        return
    }

    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
    }

    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    val previewView = remember { PreviewView(context) }

    DisposableEffect(lifecycleOwner, flashEnabled) {
        val executor = ContextCompat.getMainExecutor(context)
        val listener = Runnable {
            try {
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                imageCapture.flashMode = if (flashEnabled) {
                    ImageCapture.FLASH_MODE_ON
                } else {
                    ImageCapture.FLASH_MODE_OFF
                }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_BACK_CAMERA,
                    preview,
                    imageCapture
                )
            } catch (e: Exception) {
                captureError = "تعذر تشغيل الكاميرا على هذا الجهاز. يمكنك اختيار صورة من الهاتف."
            }
        }
        cameraProviderFuture.addListener(listener, executor)
        onDispose {
            try {
                cameraProviderFuture.get().unbindAll()
            } catch (_: Exception) {
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            factory = { previewView },
            modifier = Modifier.fillMaxSize()
        )

        // Framing Reticle for clear symbol photography
        Canvas(modifier = Modifier.fillMaxSize()) {
            val boxW = size.width * 0.78f
            val boxH = size.height * 0.42f
            val left = (size.width - boxW) / 2f
            val top = (size.height - boxH) / 2f
            drawRect(
                color = Color(0xFFD9AE5B).copy(alpha = 0.85f),
                topLeft = Offset(left, top),
                size = Size(boxW, boxH),
                style = Stroke(width = 3f)
            )
        }

        // Top controls
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.55f)
            ) {
                IconButton(onClick = onClose, modifier = Modifier.testTag("camera_close_button")) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "إغلاق الكاميرا",
                        tint = Color.White
                    )
                }
            }
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.55f)
            ) {
                IconButton(
                    onClick = { flashEnabled = !flashEnabled },
                    modifier = Modifier.testTag("camera_flash_button")
                ) {
                    Icon(
                        imageVector = if (flashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                        contentDescription = "الفلاش",
                        tint = if (flashEnabled) Color(0xFFD9AE5B) else Color.White
                    )
                }
            }
        }

        // Bottom capture controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.65f))
                .navigationBarsPadding()
                .padding(vertical = 20.dp, horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = captureError ?: "ضع الرمز داخل الإطار الذهبي بإضاءة واضحة ودون لمس أي جسم مشبوه",
                style = MaterialTheme.typography.bodySmall,
                color = if (captureError != null) Color(0xFFFFB4AB) else Color(0xFFF4EBD9),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(14.dp))
            Surface(
                onClick = {
                    if (isCapturing) return@Surface
                    isCapturing = true
                    captureError = null
                    val outFile = imageStorageManager.createCameraOutputFile()
                    val outputOptions = ImageCapture.OutputFileOptions.Builder(outFile).build()
                    imageCapture.takePicture(
                        outputOptions,
                        ContextCompat.getMainExecutor(context),
                        object : ImageCapture.OnImageSavedCallback {
                            override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                                isCapturing = false
                                onImageCaptured(outFile.absolutePath)
                            }

                            override fun onError(exception: ImageCaptureException) {
                                isCapturing = false
                                captureError = "تعذر التقاط الصورة. حاول مرة أخرى أو اختر صورة من الهاتف."
                            }
                        }
                    )
                },
                shape = CircleShape,
                color = Color(0xFFD9AE5B),
                modifier = Modifier
                    .size(72.dp)
                    .testTag("camera_shutter_button")
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.CameraAlt,
                        contentDescription = "التقاط صورة",
                        tint = Color(0xFF1B140E),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
