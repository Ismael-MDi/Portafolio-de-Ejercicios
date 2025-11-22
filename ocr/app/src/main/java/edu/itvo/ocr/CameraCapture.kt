package edu.itvo.ocr

import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.camera.core.Preview
import androidx.compose.runtime.MutableState
import androidx.core.content.ContextCompat
import java.io.File

fun startCamera(
    context: Context,
    previewView: PreviewView,
    imageCapture: MutableState<ImageCapture?>
) {
    val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

    cameraProviderFuture.addListener({
        val cameraProvider = cameraProviderFuture.get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }

        val capture = ImageCapture.Builder().build()
        imageCapture.value = capture

        val cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA

        try {
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                context as androidx.lifecycle.LifecycleOwner,
                cameraSelector,
                preview,
                capture
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }, ContextCompat.getMainExecutor(context))
}

fun takePhoto(
    context: Context,
    imageCapture: ImageCapture?,
    onImageSaved: (Uri) -> Unit
) {
    val file = File(context.externalMediaDirs.first(), "captured.jpg")

    val outputOptions = ImageCapture.OutputFileOptions.Builder(file).build()

    imageCapture?.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                exc.printStackTrace()
            }

            override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                onImageSaved(Uri.fromFile(file))
            }
        }
    )
}
