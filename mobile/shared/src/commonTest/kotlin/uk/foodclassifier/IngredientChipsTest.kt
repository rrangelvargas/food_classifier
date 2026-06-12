package uk.foodclassifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IngredientChipsTest {
    @Test
    fun parsesGarbledCameraOcrIntoMultipleChips() {
        val garbled =
            "B-NMGREDIENTS:Protein Blend (Calcium Caseinate (MUK, NL-IM Whey Protein Isolate Milk;; " +
                "Sweeteners: Maltitol, Sucralose; Bovine Collagen Hydrolysate;; " +
                "Humectant: Glycerol; Water, PRln, Cocoa Butter, Whole Milk Powder;; " +
                "Emulsifier Soy Lecithin; Panuings. ALLERGN VICE,"

        val chips = parseIngredientsToChips(garbled)

        assertTrue(chips.size > 5, "Expected multiple chips but got: $chips")
        assertTrue(chips.any { it.contains("Maltitol", ignoreCase = true) })
        assertTrue(chips.any { it.contains("Glycerol", ignoreCase = true) })
        assertTrue(chips.any { it.contains("Collagen", ignoreCase = true) })
        assertFalse(chips.any { it.contains("B-NMGREDIENTS", ignoreCase = true) })
    }

    @Test
    fun parsesMultilineExtractedIngredientsIntoChips() {
        val chips = parseIngredientsToChips(
            """
            Reduced Fat Soft Cheese (Milk),
            Salt,
            Stabilisers: Guar Gum;
            Carrageenan,
            """.trimIndent(),
        )
        assertEquals(4, chips.size)
        assertEquals("Reduced Fat Soft Cheese (Milk)", chips[0])
        assertEquals("Guar Gum", chips[2])
    }

    @Test
    fun stripsFunctionalLabelsFromChips() {
        val chips = parseIngredientsToChips(
            "Sweeteners: Maltitol, Sucralose, Emulsifier: Soy Lecithin, Acidity Regulator: Sodium Hydroxide",
        )
        assertEquals(
            listOf("Maltitol", "Sucralose", "Soy Lecithin", "Sodium Hydroxide"),
            chips,
        )
    }

    @Test
    fun formatsChipsForClassification() {
        assertEquals(
            "water, oats, salt",
            formatIngredientsFromChips(listOf("water", "oats", "salt")),
        )
    }
}
