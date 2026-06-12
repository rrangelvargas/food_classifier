package uk.foodclassifier.image

import android.graphics.Bitmap
import android.graphics.Color

internal fun invertAndEnhanceContrast(source: Bitmap): Bitmap {
    val width = source.width
    val height = source.height
    val output = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val pixels = IntArray(width * height)
    source.getPixels(pixels, 0, width, 0, 0, width, height)

    for (index in pixels.indices) {
        val pixel = pixels[index]
        val alpha = Color.alpha(pixel)
        val red = Color.red(pixel)
        val green = Color.green(pixel)
        val blue = Color.blue(pixel)
        var gray = (0.299 * red + 0.587 * green + 0.114 * blue).toInt()
        gray = 255 - gray
        gray = ((gray - 128) * 1.8 + 128).toInt().coerceIn(0, 255)
        pixels[index] = Color.argb(alpha, gray, gray, gray)
    }

    output.setPixels(pixels, 0, width, 0, 0, width, height)
    return output
}
