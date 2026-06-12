package uk.foodclassifier.image

import kotlin.test.Test
import kotlin.test.assertEquals

class NormalizedRectTest {
    @Test
    fun translatedClampsToImageBounds() {
        val rect = NormalizedRect(0.1f, 0.2f, 0.5f, 0.7f)
        val moved = rect.translated(normalizedDeltaX = 0.8f, normalizedDeltaY = 0.8f)
        assertEquals(1.0f, moved.right)
        assertEquals(1.0f, moved.bottom)
    }

    @Test
    fun fitImageBoundsLetterboxesWideImage() {
        val bounds = fitImageBounds(
            containerWidth = 100f,
            containerHeight = 200f,
            imageWidth = 200f,
            imageHeight = 100f,
        )
        assertEquals(75f, bounds.top)
        assertEquals(50f, bounds.height)
    }

    @Test
    fun mapViewGuideToImageGuideAccountsForLetterboxing() {
        val guide = NormalizedRect(0.04f, 0.36f, 0.96f, 0.58f)
        val mapped = mapViewGuideToImageGuide(
            guide = guide,
            viewWidth = 100f,
            viewHeight = 200f,
            imageWidth = 200f,
            imageHeight = 100f,
        )
        assertEquals(0.04f, mapped.left)
        assertEquals(0.0f, mapped.top)
        assertEquals(0.96f, mapped.right)
        assertEquals(0.82f, mapped.bottom)
    }
}
