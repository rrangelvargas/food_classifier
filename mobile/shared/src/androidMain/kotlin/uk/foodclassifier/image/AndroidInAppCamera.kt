package uk.foodclassifier.image

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.UseCaseGroup
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executor

@Composable
actual fun InAppCameraScreen(
    visible: Boolean,
    onCaptured: (CameraCaptureResult) -> Unit,
    onDismiss: () -> Unit,
) {
    if (!visible) {
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainExecutor = remember(context) { ContextCompat.getMainExecutor(context) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var previewContentWidth by remember { mutableStateOf<Float?>(null) }
    var previewContentHeight by remember { mutableStateOf<Float?>(null) }
    var isCapturing by remember { mutableStateOf(false) }
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            onDismiss()
        }
    }

    DisposableEffect(visible) {
        if (visible && !hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
        onDispose { }
    }

    DisposableEffect(previewView, hasCameraPermission, lifecycleOwner) {
        val view = previewView
        if (!hasCameraPermission || view == null) {
            return@DisposableEffect onDispose { }
        }

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)

        fun bindCamera() {
            val viewPort = view.viewPort ?: return
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also { useCase ->
                useCase.surfaceProvider = view.surfaceProvider
            }
            val capture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                .build()
                .also { useCase ->
                    useCase.targetRotation = view.display.rotation
                }
            imageCapture = capture
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                UseCaseGroup.Builder()
                    .setViewPort(viewPort)
                    .addUseCase(preview)
                    .addUseCase(capture)
                    .build(),
            )
            updatePreviewContentSize(view) { width, height ->
                previewContentWidth = width
                previewContentHeight = height
            }
        }

        val listener = Runnable { bindCamera() }
        cameraProviderFuture.addListener(listener, mainExecutor)
        view.post { bindCamera() }

        onDispose {
            imageCapture = null
            previewContentWidth = null
            previewContentHeight = null
            runCatching { cameraProviderFuture.get().unbindAll() }
        }
    }

    if (!hasCameraPermission) {
        return
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = Color.Black,
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
            ) {
                TextButton(onClick = onDismiss, enabled = !isCapturing) {
                    Text("Cancel", color = Color.White)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FIT_CENTER
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        }
                    },
                    update = { view ->
                        previewView = view
                        updatePreviewContentSize(view) { width, height ->
                            previewContentWidth = width
                            previewContentHeight = height
                        }
                    },
                )

                CropGuideOverlay(
                    guide = NormalizedRect.ingredientsGuide,
                    modifier = Modifier.fillMaxSize(),
                    contentWidth = previewContentWidth,
                    contentHeight = previewContentHeight,
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    enabled = !isCapturing,
                ) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val capture = imageCapture ?: return@Button
                        val view = previewView ?: return@Button
                        isCapturing = true
                        capture.takePicture(
                            mainExecutor,
                            CaptureCallback(
                                executor = mainExecutor,
                                previewView = view,
                                onSuccess = { result ->
                                    isCapturing = false
                                    onCaptured(result)
                                },
                                onError = {
                                    isCapturing = false
                                },
                            ),
                        )
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isCapturing && imageCapture != null,
                ) {
                    Text(if (isCapturing) "Capturing…" else "Capture")
                }
            }
        }
    }
}

private class CaptureCallback(
    private val executor: Executor,
    private val previewView: PreviewView,
    private val onSuccess: (CameraCaptureResult) -> Unit,
    private val onError: () -> Unit,
) : ImageCapture.OnImageCapturedCallback() {
    override fun onCaptureSuccess(image: ImageProxy) {
        val result = buildCaptureResult(image, previewView)
        image.close()
        if (result != null) {
            executor.execute { onSuccess(result) }
        } else {
            executor.execute { onError() }
        }
    }

    override fun onError(exception: ImageCaptureException) {
        executor.execute { onError() }
    }
}

private fun buildCaptureResult(image: ImageProxy, previewView: PreviewView): CameraCaptureResult? {
    val rawBytes = imageProxyToJpegBytes(image) ?: return null
    val normalizedBytes = normalizeJpegOrientation(rawBytes)
    val bitmap = BitmapFactory.decodeByteArray(normalizedBytes, 0, normalizedBytes.size) ?: return null
    val initialCrop = mapViewGuideToImageGuide(
        guide = NormalizedRect.ingredientsGuide,
        viewWidth = previewView.width.toFloat(),
        viewHeight = previewView.height.toFloat(),
        imageWidth = bitmap.width.toFloat(),
        imageHeight = bitmap.height.toFloat(),
    )
    bitmap.recycle()
    return CameraCaptureResult(
        imageBytes = normalizedBytes,
        initialCrop = initialCrop,
    )
}

private fun updatePreviewContentSize(
    view: PreviewView,
    onSize: (width: Float, height: Float) -> Unit,
) {
    val previewBitmap = view.bitmap ?: return
    onSize(previewBitmap.width.toFloat(), previewBitmap.height.toFloat())
}

private fun imageProxyToJpegBytes(image: ImageProxy): ByteArray? {
    val buffer = image.planes.firstOrNull()?.buffer ?: return null
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return bytes.takeIf { it.isNotEmpty() }
}

internal fun cropBitmapToNormalizedRect(bitmap: Bitmap, crop: NormalizedRect): Bitmap? {
    val left = (crop.left * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
    val top = (crop.top * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
    val right = (crop.right * bitmap.width).toInt().coerceIn(left + 1, bitmap.width)
    val bottom = (crop.bottom * bitmap.height).toInt().coerceIn(top + 1, bitmap.height)
    return Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
}

private fun Bitmap.toJpegByteArray(): ByteArray {
    val output = ByteArrayOutputStream()
    compress(Bitmap.CompressFormat.JPEG, 90, output)
    return output.toByteArray()
}
