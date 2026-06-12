package uk.foodclassifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ClassificationMessagesTest {
    @Test
    fun describesDairyInPlainLanguage() {
        val messages = describeClassificationMatches(emptyList(), listOf("milk", "whey"))
        assertEquals(1, messages.size)
        assertTrue(messages[0].contains("cow's milk or other dairy"))
    }

    @Test
    fun describesBovineInPlainLanguage() {
        val messages = describeClassificationMatches(listOf("bovine", "collagen"), emptyList())
        assertEquals(1, messages.size)
        assertTrue(messages[0].contains("derived from cattle"))
    }

    @Test
    fun describesMilkAndBovineSeparately() {
        val messages = describeClassificationMatches(
            matchedNonVegetarian = listOf("bovine"),
            matchedNonVegan = listOf("milk"),
        )
        assertEquals(2, messages.size)
        assertTrue(messages.any { it.contains("cattle") })
        assertTrue(messages.any { it.contains("dairy") })
    }

    @Test
    fun describesChickenInPlainLanguage() {
        val messages = describeClassificationMatches(listOf("chicken"), emptyList())
        assertEquals(1, messages.size)
        assertTrue(messages[0].contains("poultry"))
    }

    @Test
    fun describesLikelyVeganWhenUncertain() {
        val message = describeUncertainResult(DietClassification.Vegan)
        assertTrue(message.contains("likely vegan"))
        assertTrue(message.contains("Check the label"))
    }

    @Test
    fun describesLikelyVegetarianWhenUncertain() {
        val message = describeUncertainResult(DietClassification.Vegetarian)
        assertTrue(message.contains("likely vegetarian"))
        assertTrue(message.contains("Check the label"))
    }
}
