package uk.foodclassifier.image

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.File

@Composable
actual fun rememberImagePicker(
    onGalleryPicked: (ByteArray?) -> Unit,
    onCameraPicked: (ByteArray?) -> Unit,
): ImagePickerHandle {
    val context = LocalContext.current
    var cameraUri by remember { mutableStateOf<Uri?>(null) }
    var cameraFile by remember { mutableStateOf<File?>(null) }

    val galleryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        onGalleryPicked(uri?.let { readUriAsJpeg(context, it) })
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.TakePicture(),
    ) { success ->
        val file = cameraFile
        val uri = cameraUri
        cameraFile = null
        cameraUri = null

        if (success && uri != null) {
            val bytes = readUriAsJpeg(context, uri)
            deleteTempCapture(file)
            onCameraPicked(bytes)
        } else {
            deleteTempCapture(file)
            onCameraPicked(null)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            val capture = createCameraCapture(context)
            cameraUri = capture.uri
            cameraFile = capture.file
            cameraLauncher.launch(capture.uri)
        } else {
            onCameraPicked(null)
        }
    }

    return ImagePickerHandle(
        pickFromGallery = {
            galleryLauncher.launch(
                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
            )
        },
        takePhoto = {
            when {
                ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                    PackageManager.PERMISSION_GRANTED -> {
                    val capture = createCameraCapture(context)
                    cameraUri = capture.uri
                    cameraFile = capture.file
                    cameraLauncher.launch(capture.uri)
                }
                else -> permissionLauncher.launch(Manifest.permission.CAMERA)
            }
        },
    )
}

private data class CameraCapture(
    val uri: Uri,
    val file: File,
)

private fun createCameraCapture(context: Context): CameraCapture {
    val imagesDir = File(context.cacheDir, "images").apply { mkdirs() }
    val imageFile = File(imagesDir, "capture-${System.currentTimeMillis()}.jpg")
    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        imageFile,
    )
    return CameraCapture(uri, imageFile)
}

private fun deleteTempCapture(file: File?) {
    file?.delete()
}

private fun readUriAsJpeg(context: Context, uri: Uri): ByteArray? {
    val rawBytes = context.contentResolver.openInputStream(uri)?.use { stream ->
        stream.readBytes()
    } ?: return null

    return normalizeJpegOrientation(rawBytes)
}
