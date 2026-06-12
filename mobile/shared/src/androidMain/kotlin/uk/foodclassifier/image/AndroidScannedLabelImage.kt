package uk.foodclassifier.image

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

actual fun decodeImageBitmap(bytes: ByteArray): ImageBitmap? {
    return decodeOrientedBitmap(bytes)?.asImageBitmap()
}
