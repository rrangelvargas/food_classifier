package uk.foodclassifier.image

import androidx.compose.runtime.Composable

class ImagePickerHandle(
    val pickFromGallery: () -> Unit,
    val takePhoto: () -> Unit,
)

@Composable
expect fun rememberImagePicker(
    onGalleryPicked: (ByteArray?) -> Unit,
    onCameraPicked: (ByteArray?) -> Unit,
): ImagePickerHandle
