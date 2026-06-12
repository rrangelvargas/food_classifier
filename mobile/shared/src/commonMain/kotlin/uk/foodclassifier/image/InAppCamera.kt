package uk.foodclassifier.image

import androidx.compose.runtime.Composable

@Composable
expect fun InAppCameraScreen(
    visible: Boolean,
    onCaptured: (CameraCaptureResult) -> Unit,
    onDismiss: () -> Unit,
)
