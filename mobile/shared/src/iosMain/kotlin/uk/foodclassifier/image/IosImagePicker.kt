@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package uk.foodclassifier.image

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.interop.LocalUIViewController
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.usePinned
import platform.Foundation.NSData
import platform.posix.memcpy
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.UIKit.UIImageJPEGRepresentation
import platform.UIKit.UIImagePickerController
import platform.UIKit.UIImagePickerControllerDelegateProtocol
import platform.UIKit.UIImagePickerControllerOriginalImage
import platform.UIKit.UIImagePickerControllerSourceType
import platform.UIKit.UINavigationControllerDelegateProtocol
import platform.darwin.NSObject

@Composable
actual fun rememberImagePicker(
    onGalleryPicked: (ByteArray?) -> Unit,
    onCameraPicked: (ByteArray?) -> Unit,
): ImagePickerHandle {
    val viewController = LocalUIViewController.current
    val galleryDelegate = remember {
        ImagePickerDelegate(onGalleryPicked)
    }
    val cameraDelegate = remember {
        ImagePickerDelegate(onCameraPicked)
    }

    return ImagePickerHandle(
        pickFromGallery = {
            presentPicker(
                viewController = viewController,
                delegate = galleryDelegate,
                sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypePhotoLibrary,
            )
        },
        takePhoto = {
            if (UIImagePickerController.isSourceTypeAvailable(
                    UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera,
                )
            ) {
                presentPicker(
                    viewController = viewController,
                    delegate = cameraDelegate,
                    sourceType = UIImagePickerControllerSourceType.UIImagePickerControllerSourceTypeCamera,
                )
            } else {
                onCameraPicked(null)
            }
        },
    )
}

private fun presentPicker(
    viewController: platform.UIKit.UIViewController,
    delegate: ImagePickerDelegate,
    sourceType: UIImagePickerControllerSourceType,
) {
    val picker = UIImagePickerController()
    picker.delegate = delegate
    picker.sourceType = sourceType
    viewController.presentViewController(picker, animated = true, completion = null)
}

private class ImagePickerDelegate(
    private val onImagePicked: (ByteArray?) -> Unit,
) : NSObject(), UIImagePickerControllerDelegateProtocol, UINavigationControllerDelegateProtocol {
    override fun imagePickerController(
        picker: UIImagePickerController,
        didFinishPickingMediaWithInfo: Map<Any?, *>,
    ) {
        picker.dismissViewControllerAnimated(true, completion = null)
        val image = didFinishPickingMediaWithInfo[UIImagePickerControllerOriginalImage] as? UIImage
        onImagePicked(image?.toJpegByteArray())
    }

    override fun imagePickerControllerDidCancel(picker: UIImagePickerController) {
        picker.dismissViewControllerAnimated(true, completion = null)
        onImagePicked(null)
    }
}

private fun UIImage.toJpegByteArray(): ByteArray? {
    val data = UIImageJPEGRepresentation(this, 0.9) ?: return null
    return data.toByteArray()
}

private fun NSData.toByteArray(): ByteArray {
    val size = length.toInt()
    if (size == 0) {
        return ByteArray(0)
    }
    return ByteArray(size).also { bytes ->
        bytes.usePinned { pinned ->
            memcpy(pinned.addressOf(0), this@toByteArray.bytes, size.convert())
        }
    }
}
