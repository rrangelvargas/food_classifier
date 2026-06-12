package uk.foodclassifier

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ClassifierTest {
    @Test
    fun veganIngredients() {
        val result = classifyIngredients("water, oats, sunflower oil, salt")
        assertEquals(DietClassification.Vegan, result.classification)
        assertFalse(result.hasWarnings)
    }

    @Test
    fun vegetarianIngredients() {
        val result = classifyIngredients("flour, milk, sugar, butter")
        assertEquals(DietClassification.Vegetarian, result.classification)
        assertTrue(result.matchedNonVegan.contains("milk"))
    }

    @Test
    fun nonVegetarianIngredients() {
        val result = classifyIngredients("tomatoes, chicken stock, basil")
        assertEquals(DietClassification.Neither, result.classification)
        assertTrue(result.matchedNonVegetarian.contains("chicken"))
    }

    @Test
    fun parmigianoReggianoIsUncertain() {
        val result = classifyIngredients("Parmigiano Reggiano Cheese (Cows' Milk)")
        assertEquals(DietClassification.Uncertain, result.classification)
        assertEquals(DietClassification.Vegetarian, result.definitiveClassification)
    }

    @Test
    fun bovineCollagenIsNeither() {
        val result = classifyIngredients("Bovine Collagen Hydrolysate, milk")
        assertEquals(DietClassification.Neither, result.classification)
        assertTrue(result.matchedNonVegetarian.contains("bovine"))
    }

    @Test
    fun parentheticalMilkIsDetected() {
        val result = classifyIngredients("Whey Protein Isolate (MILK), cocoa")
        assertEquals(DietClassification.Vegetarian, result.classification)
        assertTrue(result.matchedNonVegan.contains("milk"))
    }

    @Test
    fun parentheticalMilkOnlyIsDetected() {
        val result = classifyIngredients("Protein Blend (Calcium Caseinate (Milk))")
        assertEquals(DietClassification.Vegetarian, result.classification)
        assertTrue(result.matchedNonVegan.contains("milk"))
    }

    @Test
    fun squareBracketMilkIsDetected() {
        val result = classifyIngredients(
            "Protein Blend [Calcium Caseinate (Milk), Whey Protein Isolate (Milk)]",
        )
        assertEquals(DietClassification.Vegetarian, result.classification)
        assertTrue(result.matchedNonVegan.contains("milk"))
    }

    @Test
    fun ocrStyleMilkIsDetected() {
        val result = classifyIngredients(
            "Protein Blend [Calcium Caseinate Mi ME-NGR, Whey Protein Isolate lMilk]",
        )
        assertEquals(DietClassification.Vegetarian, result.classification)
        assertTrue(result.matchedNonVegan.contains("milk"))
    }

    @Test
    fun softCheeseOcrExtractClassifiesAsVegetarian() {
        val ocrText =
            "REDUCED FAT SOFT CHEESE NUTRITION:Typical v 632kJ/152kcal. Fat 10 " +
                "(arbohydrate 3.8a, of Protein 95a, Sat 070 Energy 190k/46kcal " +
                "NGREDIENTS: Reduced Fat Soft C(hese (Milk), Salt Stablisers: " +
                "Guar Gum, Carrageenan; Citrus Fibre"

        val parsed = uk.foodclassifier.text.IngredientLabelParser.parse(ocrText)
        val result = classifyIngredients(parsed.chips)
        assertEquals(DietClassification.Vegetarian, result.classification)
        assertTrue(
            result.matchedNonVegan.any { it in setOf("milk", "cheese") },
        )
    }
}
