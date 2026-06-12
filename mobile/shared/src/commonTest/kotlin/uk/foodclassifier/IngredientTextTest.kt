package uk.foodclassifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import uk.foodclassifier.text.IngredientLabelParser
import uk.foodclassifier.text.OcrNormalizer

class IngredientTextTest {
    @Test
    fun splitRespectingBracketsKeepsNestedStructure() {
        val parts = splitRespectingBrackets(
            "Protein Blend [Calcium Caseinate (Milk), Whey Protein Isolate (Milk)]",
        )
        assertEquals(1, parts.size)
        assertTrue(parts[0].contains("["))
        assertTrue(parts[0].contains("(Milk)"))
    }

    @Test
    fun extractsMilkFromSquareAndRoundBrackets() {
        val raw =
            "Protein Blend [Calcium Caseinate (Milk), Whey Protein Isolate (Milk)]"
        val segments = ingredientSegmentsForMatching(raw)
        assertTrue(segments.any { it.contains("milk") })
    }

    @Test
    fun repairsOcrMilkArtifacts() {
        val segments = ingredientSegmentsForMatching(
            "Whey Protein Isolate lMilk, Calcium Caseinate Mi ME-NGR",
        )
        assertTrue(segments.any { it.contains("milk") })
    }

    @Test
    fun structuralRepairKeepsWordSpacing() {
        val input = "Reduced Fat Soft C(hese (Milk), Salt Stablisers: Guar Gum"
        val structural = OcrNormalizer.normalize(input)
        assertEquals(
            "Reduced Fat Soft Cheese (Milk), Salt, Stablisers: Guar Gum",
            structural,
        )
    }

    @Test
    fun parserFormatsIngredientsOnePerLine() {
        assertEquals(
            "Reduced Fat Soft Cheese (Milk)\nSalt\nGuar Gum",
            IngredientLabelParser.parseBlock(
                "Reduced Fat Soft C(hese (Milk), Salt Stablisers: Guar Gum",
            ).displayText,
        )
    }
}
