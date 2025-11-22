package edu.itvo.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition

fun recognizeText(bitmap: Bitmap, onResult: (String) -> Unit) {
    val recognizer = TextRecognition.getClient()
    val image = InputImage.fromBitmap(bitmap, 0)

    recognizer.process(image)
        .addOnSuccessListener { visionText ->
            onResult(visionText.text.ifBlank { "No se detectó texto." })
        }
        .addOnFailureListener {
            onResult("Error al procesar la imagen.")
        }
}
