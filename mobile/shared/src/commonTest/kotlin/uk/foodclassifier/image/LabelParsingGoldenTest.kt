package uk.foodclassifier.image

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import uk.foodclassifier.DietClassification
import uk.foodclassifier.classifyIngredients
import uk.foodclassifier.text.IngredientLabelParser

class LabelParsingGoldenTest {
    private val softCheeseOcr =
        "REDUCED FAT SOFT CHEESE NUTRITION:Typical v 632kJ/152kcal. Fat 10 " +
            "(arbohydrate 3.8a, of Protein 95a, Sat 070 Energy 190k/46kcal " +
            "NGREDIENTS: Reduced Fat Soft C(hese (Milk), Salt Stablisers: " +
            "Guar Gum, Carrageenan; Citrus Fibre"

    private val proteinBarOcr =
        "Protein Blend [Calcium Caseinate (Milk]. NL-INGREL Whey Protern Isolate Milk)), " +
            "Sweeteners: Maltitol, Sucralose: wei-eiwitiso ne Lolagen Hydrolysate. " +
            "Humectant: Glycerol; Water, rundercolla Palm Di, Cocoa Butter: Whole Milk Powder, " +
            "Fat-reduced water, palr Cocoa Powder (3%). Wheat Flour. Rapeseed Oil, " +
            "Bulking cacaopoed Agen: Polydlextrose: Wheat Starch, Emulsifier: Soy Lecithin; " +
            "polydextroe BI, kaising Agents: Ammonium Carbonates, Sodium rijsmiddele nates; " +
            "Acidity Requlator: Sodium Hydroxide: zuurtereg ALLERGI glutenbeve Fanurings. " +
            "including cereals cee inaredients in bld MaK also ALLERGY AyICE"

    private val expectedSoftCheese = listOf(
        "Reduced Fat Soft Cheese (Milk)",
        "Salt",
        "Guar Gum",
        "Carrageenan",
        "Citrus Fibre",
    )

    private val expectedProteinBar = listOf(
        "Protein Blend [Calcium Caseinate (Milk), Whey Protein Isolate (Milk)]",
        "Maltitol",
        "Sucralose",
        "Bovine Collagen Hydrolysate",
        "Glycerol",
        "Water",
        "Palm Oil",
        "Cocoa Butter",
        "Whole Milk Powder",
        "Fat-reduced Cocoa Powder (3%)",
        "Wheat Flour",
        "Rapeseed Oil",
        "Polydextrose",
        "Wheat Starch",
        "Soy Lecithin",
        "Salt",
        "Ammonium Carbonates",
        "Sodium Carbonates",
        "Sodium Hydroxide",
        "Flavourings",
    )

    @Test
    fun parsesSoftCheeseLabelFromCameraOcr() {
        val cameraOcr =
            "REDUCED FAT SOFT CHEESE NUTRITION:Typical v 632kJ/152kcal " +
                "NGREDIENTS: Reduced Fat Soft C(hese (Milk), Salt Sabilisers: " +
                "6uar Gum, Carageenan; (itrus Fibre"

        val parsed = IngredientLabelParser.parse(cameraOcr)
        assertEquals(expectedSoftCheese, parsed.chips)
    }

    @Test
    fun parsesSoftCheeseLabelFromGarbledOcr() {
        val parsed = IngredientLabelParser.parse(softCheeseOcr)
        assertEquals(expectedSoftCheese, parsed.chips)
        assertFalse(parsed.displayText.contains("Stabilis", ignoreCase = true))
    }

    @Test
    fun splitsProteinBlendFromSweetenersWhenSquareBracketClosedWithParen() {
        val parsed = IngredientLabelParser.parseBlock(
            "Protein Blend [Calcium Caseinate (Milk), Whey Protern Isolate (Milk)), " +
                "Sweeteners: Maltitol, Sucralose",
        )
        assertEquals(
            listOf(
                "Protein Blend [Calcium Caseinate (Milk), Whey Protein Isolate (Milk)]",
                "Maltitol",
                "Sucralose",
            ),
            parsed.chips,
        )
    }

    @Test
    fun parsesProteinBarLabelFromCameraOcr() {
        val cameraOcr =
            "EN-INGREDIENTS: Protein Blend (Calcium Caseinate (Milk), Whey Protein Isolate (Milk)), " +
                "Sweeteners: Maltitol, Sucralose; Bovine Collagen Hydrolysate; Humectant: Glycerol; " +
                "Water, rundercollas Palm bl, Cocoa Butter, Whole Milk Powder, " +
                "Fat-reduced water palr Cocoa Powder (3%), Wheat Flour, Rapeseed Oil, " +
                "Bulking Agent: Polydextrose; Wheat Starch, Emulsifier: Soy Lecithinş polydextros a Paising Agents, " +
                "Ammonium Carbonates, Sodium rijsmiddele Cabonates; Acidity Regulator: Sodium Hydroxide; Flavourings. " +
                "ALLERGY ADVICE: For allergens see bold."

        val parsed = IngredientLabelParser.parse(cameraOcr)
        assertEquals(
            listOf(
                "Protein Blend (Calcium Caseinate (Milk), Whey Protein Isolate (Milk))",
                "Maltitol",
                "Sucralose",
                "Bovine Collagen Hydrolysate",
                "Glycerol",
                "Water",
                "Palm Oil",
                "Cocoa Butter",
                "Whole Milk Powder",
                "Fat-reduced Cocoa Powder (3%)",
                "Wheat Flour",
                "Rapeseed Oil",
                "Polydextrose",
                "Wheat Starch",
                "Soy Lecithin",
                "Salt",
                "Ammonium Carbonates",
                "Sodium Carbonates",
                "Sodium Hydroxide",
                "Flavourings",
            ),
            parsed.chips,
        )
    }

    @Test
    fun parsesProteinBarLabelFromGarbledOcr() {
        val parsed = IngredientLabelParser.parse(proteinBarOcr)
        assertEquals(expectedProteinBar, parsed.chips)
        assertFalse(parsed.displayText.contains("Sweeteners", ignoreCase = true))
        assertFalse(parsed.displayText.contains("Humectant", ignoreCase = true))
        assertFalse(parsed.displayText.contains("ALLERGY", ignoreCase = true))
    }

    @Test
    fun classifiesProteinBarWithGlycerolAndFlavouringsWarnings() {
        val parsed = IngredientLabelParser.parse(proteinBarOcr)
        val result = classifyIngredients(parsed.chips)

        assertEquals(DietClassification.Neither, result.classification)
        assertTrue(result.warnings.any { it.term == "glycerol" })
        assertTrue(result.warnings.any { it.term == "flavourings" })
    }
}
