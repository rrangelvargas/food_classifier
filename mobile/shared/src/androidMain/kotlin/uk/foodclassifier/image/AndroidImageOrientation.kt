package uk.foodclassifier.image

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

internal fun normalizeJpegOrientation(jpegBytes: ByteArray): ByteArray {
    val orientation = readExifOrientation(jpegBytes)
    if (orientation == ExifInterface.ORIENTATION_NORMAL ||
        orientation == ExifInterface.ORIENTATION_UNDEFINED
    ) {
        return jpegBytes
    }

    val source = BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size) ?: return jpegBytes
    val transformed = applyExifOrientation(source, orientation)
    if (transformed === source) {
        source.recycle()
        return jpegBytes
    }
    source.recycle()
    return transformed.toJpegByteArray().also { transformed.recycle() }
}

internal fun decodeOrientedBitmap(jpegBytes: ByteArray): Bitmap? {
    val normalized = normalizeJpegOrientation(jpegBytes)
    return BitmapFactory.decodeByteArray(normalized, 0, normalized.size)
}

private fun readExifOrientation(jpegBytes: ByteArray): Int {
    return runCatching {
        ExifInterface(ByteArrayInputStream(jpegBytes))
            .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
    }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
}

private fun applyExifOrientation(source: Bitmap, orientation: Int): Bitmap {
    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.preScale(-1f, 1f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.preScale(1f, -1f)
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.postRotate(90f)
            matrix.preScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.postRotate(270f)
            matrix.preScale(-1f, 1f)
        }
        else -> return source
    }
    return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
}

private fun Bitmap.toJpegByteArray(): ByteArray {
    val output = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, 90, output)
    return output.toByteArray()
}
