package com.example.ocr

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val permission = Manifest.permission.CAMERA
        val launcher = registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) {}

        if (ContextCompat.checkSelfPermission(this, permission)
            != PackageManager.PERMISSION_GRANTED
        ) {
            launcher.launch(permission)
        }

        setContent { OCRScreen() }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OCRScreen() {
    var textResult by remember { mutableStateOf("Texto detectado aparecerá aquí...") }
    val context = LocalContext.current

    Column(Modifier.fillMaxSize().padding(16.dp)) {

        var preview by remember { mutableStateOf<Preview?>(null) }
        var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }

        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            factory = { ctx ->
                val previewView = PreviewView(ctx)

                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({

                    val cameraProvider = cameraProviderFuture.get()
                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    preview = Preview.Builder().build().also {
                        it.setSurfaceProvider(previewView.surfaceProvider)
                    }

                    imageCapture = ImageCapture.Builder().build()

                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        ctx as ComponentActivity,
                        cameraSelector,
                        preview,
                        imageCapture
                    )

                }, ContextCompat.getMainExecutor(ctx))

                previewView
            }
        )

        Button(
            onClick = {
                val imgCap = imageCapture ?: return@Button

                imgCap.takePicture(
                    ContextCompat.getMainExecutor(context),
                    object : ImageCapture.OnImageCapturedCallback() {
                        override fun onCaptureSuccess(image: ImageProxy) {
                            val mediaImage = image.image ?: return
                            val rotation = image.imageInfo.rotationDegrees

                            val inputImage =
                                InputImage.fromMediaImage(mediaImage, rotation)

                            val recognizer = TextRecognition.getClient()
                            recognizer.process(inputImage)
                                .addOnSuccessListener { result ->
                                    textResult = result.text
                                    image.close()
                                }
                                .addOnFailureListener {
                                    textResult = "Error: ${it.message}"
                                    image.close()
                                }
                        }
                    }
                )
            }
        ) {
            Text("Tomar Foto & OCR")
        }

        Text(textResult, modifier = Modifier.padding(8.dp))
    }
}
