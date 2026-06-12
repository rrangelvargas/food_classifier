package uk.foodclassifier.image

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

actual fun cropImageBytes(imageBytes: ByteArray, crop: NormalizedRect): ByteArray? {
    val source = decodeOrientedBitmap(imageBytes) ?: return null
    val left = (crop.left * source.width).roundToInt().coerceIn(0, source.width - 1)
    val top = (crop.top * source.height).roundToInt().coerceIn(0, source.height - 1)
    val right = (crop.right * source.width).roundToInt().coerceIn(left + 1, source.width)
    val bottom = (crop.bottom * source.height).roundToInt().coerceIn(top + 1, source.height)
    val width = right - left
    val height = bottom - top

    return try {
        val cropped = Bitmap.createBitmap(source, left, top, width, height)
        val bytes = cropped.toJpegByteArray()
        cropped.recycle()
        bytes
    } finally {
        source.recycle()
    }
}

actual fun prefersInAppCamera(): Boolean = true

private fun Bitmap.toJpegByteArray(): ByteArray {
    val output = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, 90, output)
    return output.toByteArray()
}
