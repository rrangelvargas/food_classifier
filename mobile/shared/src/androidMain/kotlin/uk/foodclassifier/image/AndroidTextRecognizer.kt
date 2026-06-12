package uk.foodclassifier.image

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

actual suspend fun recognizeTextFromImage(imageBytes: ByteArray): String {
    val bitmap = decodeOrientedBitmap(imageBytes)
        ?: throw IllegalArgumentException("Could not decode image")
    val image = InputImage.fromBitmap(bitmap, 0)
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    return try {
        recognizer.process(image).await().text
    } finally {
        recognizer.close()
        bitmap.recycle()
    }
}
