@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package uk.foodclassifier.image

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
actual fun InAppCameraScreen(
    visible: Boolean,
    onCaptured: (CameraCaptureResult) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) {
        return
    }
}
