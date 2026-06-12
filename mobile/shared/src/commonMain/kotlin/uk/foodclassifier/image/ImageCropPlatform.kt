package uk.foodclassifier.image

expect fun cropImageBytes(imageBytes: ByteArray, crop: NormalizedRect): ByteArray?

expect fun prefersInAppCamera(): Boolean
