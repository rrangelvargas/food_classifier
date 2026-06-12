package uk.foodclassifier.image

data class NormalizedRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
) {
    init {
        require(left in 0f..1f && top in 0f..1f && right in 0f..1f && bottom in 0f..1f)
        require(right > left && bottom > top)
    }

    fun translated(normalizedDeltaX: Float, normalizedDeltaY: Float): NormalizedRect {
        val width = right - left
        val height = bottom - top
        var newLeft = left + normalizedDeltaX
        var newTop = top + normalizedDeltaY
        newLeft = newLeft.coerceIn(0f, 1f - width)
        newTop = newTop.coerceIn(0f, 1f - height)
        return NormalizedRect(
            left = newLeft,
            top = newTop,
            right = newLeft + width,
            bottom = newTop + height,
        )
    }

    companion object {
        /** Wide horizontal band — matches a typical ingredients block on product labels. */
        val ingredientsGuide = NormalizedRect(
            left = 0.04f,
            top = 0.36f,
            right = 0.96f,
            bottom = 0.58f,
        )

        val fullImage = NormalizedRect(
            left = 0f,
            top = 0f,
            right = 1f,
            bottom = 1f,
        )
    }
}

internal fun fitImageBounds(
    containerWidth: Float,
    containerHeight: Float,
    imageWidth: Float,
    imageHeight: Float,
): ImageBounds {
    if (containerWidth <= 0f || containerHeight <= 0f || imageWidth <= 0f || imageHeight <= 0f) {
        return ImageBounds(0f, 0f, containerWidth, containerHeight)
    }

    val imageAspect = imageWidth / imageHeight
    val containerAspect = containerWidth / containerHeight
    return if (imageAspect > containerAspect) {
        val displayedWidth = containerWidth
        val displayedHeight = containerWidth / imageAspect
        val yOffset = (containerHeight - displayedHeight) / 2f
        ImageBounds(0f, yOffset, displayedWidth, displayedHeight)
    } else {
        val displayedHeight = containerHeight
        val displayedWidth = containerHeight * imageAspect
        val xOffset = (containerWidth - displayedWidth) / 2f
        ImageBounds(xOffset, 0f, displayedWidth, displayedHeight)
    }
}

internal data class ImageBounds(
    val left: Float,
    val top: Float,
    val width: Float,
    val height: Float,
) {
    fun toScreenRect(normalized: NormalizedRect): ScreenRect = ScreenRect(
        left = left + normalized.left * width,
        top = top + normalized.top * height,
        right = left + normalized.right * width,
        bottom = top + normalized.bottom * height,
    )
}

fun mapViewGuideToImageGuide(
    guide: NormalizedRect,
    viewWidth: Float,
    viewHeight: Float,
    imageWidth: Float,
    imageHeight: Float,
): NormalizedRect {
    if (viewWidth <= 0f || viewHeight <= 0f || imageWidth <= 0f || imageHeight <= 0f) {
        return guide
    }

    val displayed = fitImageBounds(viewWidth, viewHeight, imageWidth, imageHeight)

    val guideLeftPx = guide.left * viewWidth
    val guideTopPx = guide.top * viewHeight
    val guideRightPx = guide.right * viewWidth
    val guideBottomPx = guide.bottom * viewHeight

    val contentLeft = displayed.left
    val contentTop = displayed.top
    val contentRight = displayed.left + displayed.width
    val contentBottom = displayed.top + displayed.height

    val clippedLeft = maxOf(guideLeftPx, contentLeft)
    val clippedTop = maxOf(guideTopPx, contentTop)
    val clippedRight = minOf(guideRightPx, contentRight)
    val clippedBottom = minOf(guideBottomPx, contentBottom)

    if (clippedRight <= clippedLeft || clippedBottom <= clippedTop) {
        return guide
    }

    return NormalizedRect(
        left = (clippedLeft - contentLeft) / displayed.width,
        top = (clippedTop - contentTop) / displayed.height,
        right = (clippedRight - contentLeft) / displayed.width,
        bottom = (clippedBottom - contentTop) / displayed.height,
    )
}

internal data class ScreenRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float,
)
