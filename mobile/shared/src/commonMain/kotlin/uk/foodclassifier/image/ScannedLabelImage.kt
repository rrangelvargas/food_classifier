package uk.foodclassifier.image

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

expect fun decodeImageBitmap(bytes: ByteArray): ImageBitmap?

@Composable
fun ScannedLabelImage(
    imageBytes: ByteArray?,
    modifier: Modifier = Modifier,
) {
    val bitmap = remember(imageBytes) {
        imageBytes?.let(::decodeImageBitmap)
    }
    if (bitmap != null) {
        Image(
            bitmap = bitmap,
            contentDescription = "Scanned food label",
            modifier = modifier
                .fillMaxWidth()
                .heightIn(max = 240.dp)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Fit,
        )
    }
}
