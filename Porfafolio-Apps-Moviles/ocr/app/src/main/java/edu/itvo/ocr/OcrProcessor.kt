package edu.itvo.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition

fun processImage(context: Context, uri: Uri, onResult: (String) -> Unit) {
    val image = InputImage.fromFilePath(context, uri)
    val recognizer = TextRecognition.getClient()

    recognizer.process(image)
        .addOnSuccessListener { visionText ->
            onResult(visionText.text)
        }
        .addOnFailureListener {
            onResult("Error: ${it.message}")
        }
}
