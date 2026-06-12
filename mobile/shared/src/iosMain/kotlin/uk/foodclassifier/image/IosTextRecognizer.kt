@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package uk.foodclassifier.image

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.suspendCancellableCoroutine
import platform.Foundation.NSData
import platform.Foundation.create
import platform.UIKit.UIImage
import platform.Vision.VNImageRequestHandler
import platform.Vision.VNRecognizeTextRequest
import platform.Vision.VNRecognizedText
import platform.Vision.VNRecognizedTextObservation
import platform.Vision.VNRequestTextRecognitionLevelAccurate
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

actual suspend fun recognizeTextFromImage(imageBytes: ByteArray): String {
  val uiImage = imageBytes.toUIImage()
      ?: throw IllegalArgumentException("Could not decode image")
  val cgImage = uiImage.CGImage
      ?: throw IllegalArgumentException("Could not read image data")

  return suspendCancellableCoroutine { continuation ->
    val request = VNRecognizeTextRequest { visionRequest, error ->
      if (error != null) {
        continuation.resumeWithException(
            IllegalStateException(error.localizedDescription ?: "Text recognition failed"),
        )
        return@VNRecognizeTextRequest
      }

      val lines = buildList {
        val results = visionRequest?.results.orEmpty()
        for (result in results) {
          val observation = result as? VNRecognizedTextObservation ?: continue
          val candidates = observation.topCandidates(1u) as? List<*>
          val candidate = candidates?.firstOrNull() as? VNRecognizedText ?: continue
          add(candidate.string)
        }
      }

      continuation.resume(lines.joinToString("\n"))
    }
    request.recognitionLevel = VNRequestTextRecognitionLevelAccurate

    val handler = VNImageRequestHandler(cgImage, options = emptyMap<Any?, Any?>())
    val success = handler.performRequests(listOf(request), error = null)
    if (!success) {
      continuation.resumeWithException(IllegalStateException("Vision request failed"))
    }
  }
}

private fun ByteArray.toUIImage(): UIImage? {
  val data = usePinned { pinned ->
    NSData.create(bytes = pinned.addressOf(0), length = size.toULong())
  }
  return UIImage(data = data)
}
