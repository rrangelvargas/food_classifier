package uk.foodclassifier.image

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import uk.foodclassifier.DietClassification
import uk.foodclassifier.classifyIngredients
import uk.foodclassifier.text.IngredientLabelParser

class ProteinBarRegressionTest {
    private val userExtractedText =
        "Protein Blend [Calcium Caseinate (Milk) WheyProtern Isolate (Milk);; " +
            "Sweeteners: Maltitol, Sucralose; Bovine Collagen Hydrolysate;; " +
            "Humectant: Glycerol; Water, Palm bl, Cocoa Butter, Whole Milk Powder, " +
            "Fat- water palr Cocoa Powder (3%), Wheat Flour, Rapeseed Oil, Bulking Agent- ; " +
            "Wheat Starch;; Emulsifier: Soy Lecithin a Paising Agents, Ammonium Carbonates, " +
            "Sodium Carbonates;; Acidity Regulator: Sodium Hydroxide, Fanurinas ALLERGYVICE,"

    @Test
    fun stopsExtractionAtAllergyText() {
        val rawOcr =
            "EN-INGREDIENTS: Protein Blend, Bovine Collagen Hydrolysate, Flavourings. " +
                "ALLERGY ADVICE: For allergens see bold."

        val extracted = IngredientsTextExtractor.extract(rawOcr)

        assertFalse(extracted.contains("ALLERGY", ignoreCase = true))
        assertTrue(extracted.contains("Bovine Collagen Hydrolysate"))
    }

    @Test
    fun stopsExtractionAtGarbledAllergyVice() {
        val rawOcr =
            "EN-INGREDIENTS: Protein Blend, Bovine Collagen Hydrolysate, Fanurinas ALLERGYVICE, " +
                "For allergens see bold."

        val parsed = IngredientLabelParser.parse(rawOcr)
        assertFalse(parsed.displayText.contains("ALLERGY", ignoreCase = true))
        assertTrue(parsed.chips.any { it.contains("Bovine Collagen Hydrolysate", ignoreCase = true) })
        assertFalse(parsed.displayText.contains("For allergens", ignoreCase = true))
    }

    @Test
    fun classifiesUserExtractedTextAsNeitherDueToCollagen() {
        val parsed = IngredientLabelParser.parseBlock(userExtractedText)

        assertTrue(parsed.chips.any { it.contains("Collagen", ignoreCase = true) })

        val result = classifyIngredients(parsed.chips)
        assertEquals(DietClassification.Neither, result.classification)
        assertTrue(result.matchedNonVegetarian.any { it == "bovine" || it == "collagen" })
    }

    @Test
    fun classifiesUserExtractedTextViaStringPath() {
        val parsed = IngredientLabelParser.parseBlock(userExtractedText)
        val result = classifyIngredients(parsed.chips)

        assertEquals(DietClassification.Neither, result.classification)
        assertTrue(result.matchedNonVegetarian.isNotEmpty())
    }
}
