package uk.foodclassifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import uk.foodclassifier.image.IngredientsTextExtractor

class SunscreenClassifierTest {
    private val sunscreenIngredients =
        """
        Aqua, Isopropyl Palmitate, C12-15 Alkyl Benzoate, Glycerin, Alcohol Denat.,
        Bis-Ethylhexyloxyphenol Methoxyphenyl Triazine, Butyl Methoxydibenzoylmethane,
        Dibutyl Adipate, Ethylhexyl Triazone, Copernicia Cerifera Cera, Glyceryl Stearate SE,
        Microcrystalline Cellulose, Phenylbenzimidazole Sulfonic Acid, Sodium Ascorbyl Phosphate,
        Sodium Hyaluronate, Tocopheryl Acetate, Xanthan Gum, Polyglyceryl-3 Methylglucose Distearate,
        Diisostearoyl Polyglyceryl-3 Dimer Dilinoleate, Hydrogenated Rapeseed Oil, Cetyl Palmitate,
        Cellulose Gum, Sodium Cetearyl Sulfate, Sodium Hydroxide, Trisodium Ethylenediamine Disuccinate,
        Hydroxyacetophenone, Ethylhexylglycerin, Phenoxyethanol, Linalool, Benzyl Alcohol,
        Linalyl Acetate, Alpha-Isomethyl Ionone, Citronellol, Vanillin, Parfum
        """.trimIndent()

    private fun classifySunscreen(): ClassificationResult {
        return classifyIngredients(formatIngredientsFromChips(parseIngredientsToChips(sunscreenIngredients)))
    }

    @Test
    fun doesNotWarnAboutWineInSunscreenIngredients() {
        val result = classifySunscreen()
        assertFalse(
            result.warnings.any { it.term.contains("wine") },
            "Unexpected wine warning: ${result.warnings}",
        )
    }

    @Test
    fun warnsAboutGlycerinAtMostOnceInSunscreenIngredients() {
        val result = classifySunscreen()
        val glycerolFamilyWarnings = result.warnings.filter {
            it.term in setOf("glycerol", "glycerin", "glycerine", "e422")
        }
        assertEquals(1, glycerolFamilyWarnings.size, "Warnings: ${result.warnings}")
    }

    @Test
    fun doesNotWarnAboutGlycerinInsideEthylhexylglycerin() {
        val result = classifyIngredients("ethylhexylglycerin")
        assertFalse(result.warnings.any { it.term in setOf("glycerol", "glycerin", "glycerine", "e422") })
    }

    @Test
    fun doesNotWarnAboutWineInVanillinOrAlcoholDenat() {
        assertFalse(classifyIngredients("vanillin").warnings.any { it.term.contains("wine") })
        assertFalse(classifyIngredients("alcohol denat").warnings.any { it.term.contains("wine") })
        assertFalse(classifyIngredients("benzyl alcohol").warnings.any { it.term.contains("wine") })
    }

    @Test
    fun doesNotTreatVanillinMisreadAsLanolinInFragranceTail() {
        val chips = parseIngredientsToChips(
            "Linalool, Benzyl Alcohol, Linalyl Acetate, Citronellol, Lanolin, Parfum",
        )
        assertEquals("Vanillin", chips.last { it.equals("Vanillin", ignoreCase = true) })
        assertFalse(chips.any { it.equals("Lanolin", ignoreCase = true) })

        val result = classifyIngredients(formatIngredientsFromChips(chips))
        assertTrue(result.matchedNonVegetarian.isEmpty())
        assertEquals(DietClassification.Vegan, result.definitiveClassification)
    }

    @Test
    fun doesNotTreatWineCelluloseOcrAsWine() {
        val result = classifyIngredients("Microcrysta\" Wine Cellulose")
        assertFalse(result.warnings.any { it.term == "wine" })
    }

    @Test
    fun classifiesUserOcrSunscreenListAsVegan() {
        val ocrList =
            """
            Aqua, Isopropyl Palmitate, C12-15 Alkyl Benzoate, Gy cerin, Alcohol Denat,
            Bis-Ethylhexyloxyphenol Methoxyphenyl Triozine, Copernicia Cerifera Cera,
            Glycerol Stearate, Microcrysta" Wine Cellulose, Phenoxyethanol, Linalool,
            Benzyl Alcohol, Linalyl Acetate, Citronellol, Lanolin, Parfum
            """.trimIndent()

        val result = classifyIngredients(formatIngredientsFromChips(parseIngredientsToChips(ocrList)))
        assertTrue(result.matchedNonVegetarian.isEmpty())
        assertEquals(DietClassification.Vegan, result.definitiveClassification)
        assertFalse(result.warnings.any { it.term == "wine" })
    }

    @Test
    fun extractsSunscreenIngredientsWithoutUsageInstructions() {
        val ocrText =
            """
            INGREDIENTS: Aqua, Glycerin, Alcohol Denat., Vanillin, Parfum
            IMPORTANT USAGE INSTRUCTIONS: Apply generously before sun exposure and reapply frequently
            Art.-No. 85581
            Made in Spain
            """.trimIndent()

        val extracted = IngredientsTextExtractor.extract(ocrText)
        assertTrue(extracted.contains("Glycerin"))
        assertFalse(extracted.contains("IMPORTANT USAGE", ignoreCase = true))
        assertFalse(extracted.contains("Apply generously", ignoreCase = true))

        val result = classifyIngredients(formatIngredientsFromChips(parseIngredientsToChips(extracted)))
        assertFalse(result.warnings.any { it.term.contains("wine") })
        assertEquals(1, result.warnings.count { it.term == "glycerin" })
    }
}
