package uk.foodclassifier.image

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await
import uk.foodclassifier.text.pickBetterOcrText

actual suspend fun recognizeTextFromImage(imageBytes: ByteArray): String {
    val bitmap = decodeOrientedBitmap(imageBytes)
        ?: throw IllegalArgumentException("Could not decode image")
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    return try {
        val originalText = recognizer.process(InputImage.fromBitmap(bitmap, 0)).await().text
        val inverted = invertAndEnhanceContrast(bitmap)
        val invertedText = try {
            recognizer.process(InputImage.fromBitmap(inverted, 0)).await().text
        } finally {
            inverted.recycle()
        }
        pickBetterOcrText(originalText, invertedText)
    } finally {
        recognizer.close()
        bitmap.recycle()
    }
}
