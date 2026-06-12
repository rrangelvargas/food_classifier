package uk.foodclassifier.image

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import uk.foodclassifier.text.IngredientLabelParser

class IngredientsTextExtractorTest {
    @Test
    fun extractsTextAfterIngredientsHeader() {
        val text = """
            Product name
            Ingredients: water, oats, sunflower oil, salt
            Allergens: may contain nuts
        """.trimIndent()

        val extracted = IngredientsTextExtractor.extract(text)
        assertTrue(extracted.contains("water"))
        assertTrue(extracted.contains("oats"))
        assertFalse(extracted.contains("may contain", ignoreCase = true))
    }

    @Test
    fun handlesOcrMisspelledIngredientsHeader() {
        val text = """
            Marketing copy about sun protection
            oredients: Aqua, Glycerin, Alcohol Denat.
            Art-No. 85581
        """.trimIndent()

        val extracted = IngredientsTextExtractor.extract(text)
        assertTrue(extracted.contains("Aqua"))
        assertTrue(extracted.contains("Glycerin"))
        assertFalse(extracted.contains("Art-No", ignoreCase = true))
        assertFalse(extracted.contains("Marketing", ignoreCase = true))
    }

    @Test
    fun extractsOnlyIngredientsFromSunscreenLabel() {
        val text = """
            CITRACELL-PROTECT dermatologically tested
            IMPORTANT USAGE INSTRUCTIONS: Apply generously
            oredients: Aqua, Isopropyl Palmitate, Glycerin, Alcohol Denat., Phenoxyethanol, Parfum
            4 005808423040S Art-No. 85581 200 ml e Beiersdorf UK Ltd.
        """.trimIndent()

        val extracted = IngredientsTextExtractor.extract(text)
        assertTrue(extracted.contains("Aqua"))
        assertTrue(extracted.contains("Phenoxyethanol"))
        assertTrue(extracted.contains("Parfum"))
        assertFalse(extracted.contains("USAGE", ignoreCase = true))
        assertFalse(extracted.contains("Beiersdorf", ignoreCase = true))
        assertFalse(extracted.contains("85581"))
        assertFalse(extracted.contains("005808423040"))
    }

    @Test
    fun returnsEmptyWhenNoIngredientsHeaderFound() {
        val extracted = IngredientsTextExtractor.extract("water, oats, sunflower oil")
        assertEquals("", extracted)
    }

    @Test
    fun extractsParentheticalAllergenStyleMilk() {
        val text = """
            Ingredients: Protein Blend [Calcium Caseinate (Milk), Whey Protein Isolate (Milk)]
        """.trimIndent()

        val parsed = IngredientLabelParser.parse(text)
        assertTrue(parsed.chips.any { it.contains("Protein Blend") })
        assertTrue(parsed.chips.any { it.contains("Milk", ignoreCase = true) })
    }

    @Test
    fun stopsAtAllergyAdvice() {
        val text = """
            Ingredients: Protein Blend [Calcium Caseinate (Milk)], Cocoa
            Allergy advice: Contains milk. May contain nuts.
            Nutrition information: Energy 200kJ
        """.trimIndent()

        val parsed = IngredientLabelParser.parse(text)
        assertTrue(parsed.chips.any { it.contains("Protein Blend") })
        assertTrue(parsed.chips.any { it.contains("Cocoa") })
        assertFalse(parsed.displayText.contains("Allergy", ignoreCase = true))
        assertFalse(parsed.displayText.contains("Nutrition", ignoreCase = true))
    }

    @Test
    fun stopsAtAllergenInformation() {
        val text = """
            Ingredients: oats, sugar, salt
            Allergen information: see ingredients in bold
        """.trimIndent()

        val parsed = IngredientLabelParser.parse(text)
        assertEquals(listOf("oats", "sugar", "salt"), parsed.chips)
    }

    @Test
    fun stopsAtAllergyAdviceOnSameLine() {
        val text =
            "Ingredients: water, salt Allergy advice: Contains celery"

        val parsed = IngredientLabelParser.parse(text)
        assertEquals(listOf("water", "salt"), parsed.chips)
    }

    @Test
    fun ignoresIngredientsMentionInsideAllergyAdvice() {
        val text = """
            Protein Blend [Calcium Caseinate (Milk)], Cocoa
            Allergy advice: Contains milk. For allergens see ingredients in bold.
            Nutrition information: Energy 200kJ
        """.trimIndent()

        val parsed = IngredientLabelParser.parse(text)
        assertTrue(parsed.chips.any { it.contains("Protein Blend") })
        assertTrue(parsed.chips.any { it.contains("Cocoa") })
        assertFalse(parsed.displayText.contains("bold", ignoreCase = true))
        assertFalse(parsed.displayText.contains("Nutrition", ignoreCase = true))
    }

    @Test
    fun usesTextBeforeAllergyAdviceWhenHeaderMissing() {
        val text = """
            Protein Blend [Calcium Caseinate (Milk)], Cocoa
            Allergy advice: Contains milk
        """.trimIndent()

        val parsed = IngredientLabelParser.parse(text)
        assertTrue(parsed.chips.any { it.contains("Protein Blend") })
        assertTrue(parsed.chips.any { it.contains("Cocoa") })
    }

    @Test
    fun extractsSoftCheeseLabelDespiteMergedNutritionColumn() {
        val ocrText =
            "REDUCED FAT SOFT CHEESE NUTRITION:Typical v 632kJ/152kcal. Fat 10 " +
                "(arbohydrate 3.8a, of Protein 95a, Sat 070 Energy 190k/46kcal " +
                "NGREDIENTS: Reduced Fat Soft C(hese (Milk), Salt Stablisers: " +
                "Guar Gum, Carrageenan; Citrus Fibre"

        val parsed = IngredientLabelParser.parse(ocrText)
        assertTrue(parsed.chips.contains("Reduced Fat Soft Cheese (Milk)"))
        assertTrue(parsed.chips.contains("Salt"))
        assertTrue(parsed.chips.contains("Guar Gum"))
        assertTrue(parsed.chips.contains("Carrageenan"))
        assertTrue(parsed.chips.contains("Citrus Fibre"))
    }
}
