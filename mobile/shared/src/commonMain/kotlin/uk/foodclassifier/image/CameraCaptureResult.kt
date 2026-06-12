package uk.foodclassifier.image

data class CameraCaptureResult(
    val imageBytes: ByteArray,
    val initialCrop: NormalizedRect,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as CameraCaptureResult
        return imageBytes.contentEquals(other.imageBytes) && initialCrop == other.initialCrop
    }

    override fun hashCode(): Int {
        var result = imageBytes.contentHashCode()
        result = 31 * result + initialCrop.hashCode()
        return result
    }
}
