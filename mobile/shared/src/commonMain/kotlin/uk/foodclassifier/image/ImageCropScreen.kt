package uk.foodclassifier.image

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.Image
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp

@Composable
fun ImageCropScreen(
    imageBytes: ByteArray,
    initialCrop: NormalizedRect = NormalizedRect.ingredientsGuide,
    onConfirm: (ByteArray) -> Unit,
    onRetake: () -> Unit,
    onCancel: () -> Unit,
) {
    val bitmap = remember(imageBytes) { decodeImageBitmap(imageBytes) }
    if (bitmap == null) {
        onCancel()
        return
    }

    var cropRect by remember(imageBytes, initialCrop) { mutableStateOf(initialCrop) }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeContentPadding(),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
                TextButton(onClick = onRetake) {
                    Text("Retake")
                }
            }

            CropImageCanvas(
                bitmap = bitmap,
                cropRect = cropRect,
                onCropRectChange = { cropRect = it },
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            )

            Button(
                onClick = {
                    val cropped = cropImageBytes(imageBytes, cropRect) ?: imageBytes
                    onConfirm(cropped)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text("Scan")
            }
        }
    }
}

@Composable
internal fun CropGuideOverlay(
    guide: NormalizedRect,
    modifier: Modifier = Modifier,
    contentWidth: Float? = null,
    contentHeight: Float? = null,
) {
    Canvas(modifier = modifier) {
        val contentBounds = if (contentWidth != null && contentHeight != null) {
            fitImageBounds(size.width, size.height, contentWidth, contentHeight)
        } else {
            ImageBounds(0f, 0f, size.width, size.height)
        }
        val screenRect = contentBounds.toScreenRect(guide)
        val hole = Rect(
            offset = Offset(screenRect.left, screenRect.top),
            size = Size(
                width = screenRect.right - screenRect.left,
                height = screenRect.bottom - screenRect.top,
            ),
        )
        val overlayPath = Path().apply {
            fillType = PathFillType.EvenOdd
            addRect(Rect(Offset.Zero, size))
            addRect(hole)
        }
        drawPath(overlayPath, color = Color.Black.copy(alpha = 0.55f))
        drawRect(
            color = Color.White,
            topLeft = hole.topLeft,
            size = hole.size,
            style = Stroke(width = 3.dp.toPx()),
        )
    }
}

@Composable
private fun CropImageCanvas(
    bitmap: ImageBitmap,
    cropRect: NormalizedRect,
    onCropRectChange: (NormalizedRect) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier = modifier.background(Color.Black, androidx.compose.foundation.shape.RoundedCornerShape(12.dp)),
    ) {
        val imageBounds = fitImageBounds(
            containerWidth = constraints.maxWidth.toFloat(),
            containerHeight = constraints.maxHeight.toFloat(),
            imageWidth = bitmap.width.toFloat(),
            imageHeight = bitmap.height.toFloat(),
        )

        Box(modifier = Modifier.fillMaxSize()) {
            Image(
                bitmap = bitmap,
                contentDescription = "Photo to crop",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(imageBounds, cropRect) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            if (imageBounds.width <= 0f || imageBounds.height <= 0f) {
                                return@detectDragGestures
                            }
                            onCropRectChange(
                                cropRect.translated(
                                    normalizedDeltaX = dragAmount.x / imageBounds.width,
                                    normalizedDeltaY = dragAmount.y / imageBounds.height,
                                ),
                            )
                        }
                    },
            ) {
                val screenRect = imageBounds.toScreenRect(cropRect)
                val hole = Rect(
                    offset = Offset(screenRect.left, screenRect.top),
                    size = Size(
                        width = screenRect.right - screenRect.left,
                        height = screenRect.bottom - screenRect.top,
                    ),
                )
                val overlayPath = Path().apply {
                    fillType = PathFillType.EvenOdd
                    addRect(Rect(Offset.Zero, size))
                    addRect(hole)
                }
                drawPath(overlayPath, color = Color.Black.copy(alpha = 0.55f))
                drawRect(
                    color = Color.White,
                    topLeft = hole.topLeft,
                    size = hole.size,
                    style = Stroke(width = 3.dp.toPx()),
                )
            }
        }
    }
}
