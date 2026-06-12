package uk.foodclassifier.image

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import uk.foodclassifier.DietClassification
import uk.foodclassifier.classifyIngredients
import uk.foodclassifier.text.IngredientLabelParser

class ProteinBarOcrTest {
    private val ocrText =
        "Protein Blend [Calcium Caseinate (Milk]. NL-INGREL Whey Protern Isolate Milk)), " +
            "Sweeteners: Maltitol, Sucralose: wei-eiwitiso ne Lolagen Hydrolysate. " +
            "Humectant: Glycerol; Water, rundercolla Palm Di, Cocoa Butter: Whole Milk Powder, " +
            "Fat-reduced water, palr Cocoa Powder (3%). Wheat Flour. Rapeseed Oil, " +
            "Bulking cacaopoed Agen: Polydlextrose: Wheat Starch, Emulsifier: Soy Lecithin; " +
            "polydextroe BI, kaising Agents: Ammonium Carbonates, Sodium rijsmiddele nates; " +
            "Acidity Requlator: Sodium Hydroxide: zuurtereg ALLERGI glutenbeve Fanurings. " +
            "including cereals cee inaredients in bld MaK also ALLERGY AyICE"

    @Test
    fun extractsEnglishIngredientsFromBilingualOcr() {
        val parsed = IngredientLabelParser.parse(ocrText)

        assertTrue(parsed.chips.any { it.contains("Protein Blend") })
        assertTrue(parsed.chips.any { it.contains("Calcium Caseinate") })
        assertTrue(parsed.chips.any { it.contains("Milk", ignoreCase = true) })
        assertTrue(parsed.chips.any { it.contains("Whey Protein", ignoreCase = true) || it.contains("Protern", ignoreCase = true) })
        assertTrue(parsed.chips.any { it.contains("Maltitol") })
        assertTrue(parsed.chips.any { it.contains("Sucralose") })
        assertTrue(parsed.chips.any { it.contains("Collagen", ignoreCase = true) })
        assertTrue(parsed.chips.any { it.contains("Glycerol", ignoreCase = true) })
        assertTrue(parsed.chips.any { it.contains("Cocoa Butter", ignoreCase = true) })
        assertTrue(parsed.chips.any { it.contains("Whole Milk Powder", ignoreCase = true) })
        assertTrue(parsed.chips.any { it.contains("Wheat Flour", ignoreCase = true) })
        assertTrue(parsed.chips.any { it.contains("Soy Lecithin", ignoreCase = true) })
        assertTrue(parsed.chips.any { it.contains("Ammonium Carbonates", ignoreCase = true) })
        assertTrue(parsed.chips.any { it.contains("Sodium Carbonates", ignoreCase = true) || it.contains("nates", ignoreCase = true) })
        assertFalse(parsed.displayText.contains("NL-INGREL", ignoreCase = true))
        assertFalse(parsed.displayText.contains("wei-eiwitiso", ignoreCase = true))
        assertFalse(parsed.displayText.contains("ALLERGY", ignoreCase = true))
    }

    @Test
    fun classifiesProteinBarAsNeitherDueToBovineCollagen() {
        val parsed = IngredientLabelParser.parse(ocrText)
        val result = classifyIngredients(parsed.chips)
        assertEquals(DietClassification.Neither, result.classification)
        assertFalse(result.matchedNonVegetarian.isEmpty())
    }

    @Test
    fun classifiesParsedProteinBarIngredientsWithGlycerolAndFlavouringsWarnings() {
        val parsed = IngredientLabelParser.parse(ocrText)
        val result = classifyIngredients(parsed.chips)

        assertTrue(result.warnings.any { it.term == "glycerol" })
        assertTrue(result.warnings.any { it.term == "flavourings" })
    }
}
